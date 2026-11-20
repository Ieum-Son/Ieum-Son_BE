# 학습 웹소켓: 프레임 간격, 에러

기준은 FastAPI `WS /api/study/ws`다. 구현은 `studyingConnection.py`, `app/schemas/study.py`, `app/services/sign_language.py`, `app/core/config.py`.

티켓 값은 `GET /api/progress/daily`의 `sessionTicket`이다. 소켓 auth 메시지 필드는 `ticket`이다.

## 프레임 간격

프레임은 한 장씩 스트리밍하지 않는다. `type`이 `attempt`인 메시지 하나에 `frames` 배열을 넣는다.

```json
{
  "type": "attempt",
  "frames": [
    {
      "pose": [{ "x": 0.1, "y": 0.2, "z": 0.0, "visibility": 0.9 }],
      "left": [{ "x": 0.1, "y": 0.2, "z": 0.0 }],
      "right": [{ "x": 0.1, "y": 0.2, "z": 0.0 }]
    }
  ]
}
```

서버는 받은 배열 전체를 60장으로 다시 맞춘다. `SEQ_LENGTH`는 60이다. 60장보다 길면 처음부터 끝까지 균등하게 고르고, 짧으면 뒤를 0으로 채워 모델의 마스크가 그 구간을 무시한다. 앞 150장만 쓰고 나머지를 버리는 방식은 쓰지 않는다.

프레임 사이 시간이나 FPS는 검사하지 않는다. 코드에 FPS 상수도 없다. 배열 순서가 시간 순서다. 어깨 너비로 정규화한 뒤, 바로 앞 프레임과의 차이로 속도와 가속도를 만든다. 클라이언트가 일정한 간격으로 뽑은 연속 장면을 그 순서대로 넣어야 한다.

한 프레임의 점 순서는 MediaPipe다.

| 부위 | 개수 | 점 하나 |
|---|---|---|
| `pose` | Pose 33. 인덱스 0이 코, 11·12가 어깨, 15·16이 손목 | `x`, `y`, `z`, `visibility` |
| `left`, `right` | Hands 21 | `x`, `y`, `z` |

개수가 부족하면 뒤를 0으로 채우고, 남으면 자른다. 좌표가 비었거나 전부 0이면 채점하지 않고 아래 `NO_LANDMARKS`를 보낸다. 소켓은 닫지 않는다.

## 에러 메시지

실패는 항상 이 모양이다. `type`은 `"error"`로 고정이고, 경우는 `code`로 구분한다.

```json
{
  "type": "error",
  "code": "INVALID_TICKET",
  "message": "티켓이 유효하지 않습니다."
}
```

| 상황 | code | message | 소켓 |
|---|---|---|---|
| 인증 실패. 티켓 없음, 서명 불일치, 만료, 클레임 부족 | `INVALID_TICKET` | 티켓이 유효하지 않습니다. | 닫음 |
| 세션 만료. 연결된 뒤 `exp`가 지난 다음 메시지 | `INVALID_TICKET` | 티켓이 유효하지 않습니다. | 닫음 |
| `auth` 전에 `attempt` | `INVALID_TICKET` | 티켓이 유효하지 않습니다. | 닫음 |
| 좌표 없음, 형식 오류, `type`이 `auth`/`attempt`가 아님 | `NO_LANDMARKS` | 유효한 좌표 시퀀스가 없습니다. | 유지 |
| 모델이 로드되지 않음 | `NO_LANDMARKS` | 모델이 로드되지 않았습니다. | 유지 |
| 동시 접속이 50을 넘음 | 없음 | 없음 | 메시지 없이 닫음 |

인증 실패와 세션 만료는 같은 `type`, 같은 `code`, 같은 `message`다. `SESSION_EXPIRED` 같은 별도 값은 없다.

스프링 HTTP 에러는 이 형식과 다르다. 본문은 `{ "message": "..." }`뿐이고 `type`과 `code`가 없다.
