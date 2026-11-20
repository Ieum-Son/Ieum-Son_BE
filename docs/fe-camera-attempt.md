# 수어 카메라 채점 (FE)

카메라로 한 수어를 찍어 오늘 학습 단어와 맞는지 확인하는 흐름이다. 오늘 학습 조회는 스프링 API, 좌표 채점은 AI 웹소켓이다.

로컬 기준 주소는 스프링 `http://localhost:8080`, AI `ws://localhost:8000/api/study/ws`다. 배포 주소만 환경에 맞게 바꾸면 된다.

## 순서

1. `GET /api/progress/daily`로 오늘 단어와 `sessionTicket`을 받는다.
2. AI 소켓을 열고, 첫 메시지로 티켓을 보낸다.
3. `session` 응답이 온 뒤에만 좌표를 보낸다.
4. 수어 한 번을 `frames` 배열 하나에 담아 `attempt`로 보낸다.
5. `result`의 `wordId`로 오늘 목록의 어느 단어가 맞았는지 본다.

문장 빈칸과 학습 마무리는 이 소켓을 쓰지 않는다. 문장은 `POST /api/progress/sentences/{sentenceId}/answers`, 마무리는 `POST /api/progress/complete`다.

## 1. 오늘 학습

```
GET /api/progress/daily
Authorization: Bearer {액세스 토큰}
```

카메라에 쓰는 값만 적는다.

| 필드 | 쓰는 곳 |
|---|---|
| `sessionTicket` | 소켓 `auth.ticket`. 호출할 때마다 새로 발급된다. |
| `expiresAt` | 이 시각이 지나면 소켓이 닫힌다. 다시 daily를 호출해 티켓을 갱신한다. |
| `words[]` | 오늘 새 단어. `wordId`, `word`, `videoUrl` |
| `reviews[]` | 오늘 복습. `wordId`, `word`, `videoUrl`, `timeLimitSeconds` |

`words`와 `reviews`에 있는 단어만 카메라 정답으로 인정된다. 화면에는 이 목록의 단어를 하나씩 보여 주면 된다.

## 2. 소켓

```
WS /api/study/ws
```

텍스트 JSON만 보낸다. 바이너리 프레임, 쿼리의 토큰, `Authorization` 헤더는 쓰지 않는다. 인증은 아래 `auth` 메시지다.

연결은 하나면 된다. `result` 또는 `error`를 받은 다음 다음 시도를 보낸다.

### auth

연결 직후 첫 메시지다.

```json
{ "type": "auth", "ticket": "<sessionTicket>" }
```

성공하면 서버가 이 메시지를 보낸다.

```json
{
  "type": "session",
  "memberId": "10",
  "sid": 99,
  "date": "2026-10-28",
  "words": ["가능", "가입"],
  "reviews": ["가족"]
}
```

`words`, `reviews`는 한국어 단어 문자열이다. 아이디는 daily 응답의 `wordId`를 쓴다. 이 메시지가 오기 전에 `attempt`를 보내면 소켓이 닫힌다.

## 3. 좌표를 뽑는 방법

MediaPipe Holistic 랜드마크를 그대로 보낸다. 픽셀 좌표로 바꾸거나, 어깨 기준으로 정규화하거나, 속도·가속도를 계산해서 보내지 않는다. 그 계산은 서버가 한다.

한 프레임:

```json
{
  "pose": [{ "x": 0.5, "y": 0.4, "z": -0.2, "visibility": 0.9 }],
  "left": [{ "x": 0.4, "y": 0.6, "z": 0.0 }],
  "right": [{ "x": 0.6, "y": 0.6, "z": 0.0 }]
}
```

| 필드 | 개수 | 점 하나 | 순서 |
|---|---|---|---|
| `pose` | 33 | `x`, `y`, `z`, `visibility` | MediaPipe Pose. 0 코, 11·12 어깨, 15·16 손목 |
| `left` | 21 | `x`, `y`, `z` | MediaPipe Hands. 0이 손목 |
| `right` | 21 | 같음 | 같음 |

- 값은 MediaPipe가 주는 정규화 좌표다. `x`, `y`는 0~1 근처다.
- 배열 인덱스가 관절 번호다. 순서를 바꾸지 않는다.
- 손이 안 보이면 그 손 배열은 `[]`로 둔다. 포즈는 33개를 채운다.
- 점이 33·21보다 적으면 서버가 뒤를 0으로 채우고, 더 많으면 뒤를 버린다. 0으로 채워진 관절은 없는 것으로 처리되므로, 감지된 점은 빠뜨리지 않는다.

한 번의 수어는 프레임을 나눠 스트리밍하지 않는다. 동작이 시작하는 장부터 끝나는 장까지를 시간 순서대로 `frames`에 넣어 메시지 하나로 보낸다.

서버는 그 배열 전체를 60장으로 맞춘다.

- 60장보다 길면 첫 장과 마지막 장을 포함한 채 균등하게 고른다.
- 60장보다 짧으면 뒤를 0으로 채워, 모델이 빈 구간을 무시한다.
- 장과 장 사이의 시간은 검사하지 않는다. 배열 순서가 시간이다. 일정한 간격으로 찍은 연속 장면이어야 한다.

수어 전체를 약 60장으로 담으면 중간 장이 빠지지 않는다. 더 촘촘히 찍어도 동작 전체는 반영된다. 앞부분만 잘라 150장 또는 60장을 보내는 방식은 동작 끝이 빠진다.

## 4. attempt

```json
{
  "type": "attempt",
  "frames": [ { "pose": [], "left": [], "right": [] } ]
}
```

`target`을 넣어도 서버는 보지 않는다. 화면의 목표 단어와 맞았는지는 응답의 `wordId`로 프론트가 비교한다.

서버는 인식 결과가 오늘 `words` 또는 `reviews` 중 하나와 같으면 그 단어를 맞힌 것으로 저장한다. 오늘 목록에 없는 단어로 인식되면 저장하지 않는다.

## 5. result

```json
{
  "type": "result",
  "target": "가능",
  "predicted": "가능1",
  "confidence": 0.97,
  "correct": true,
  "wordId": 1146,
  "recorded": true,
  "passed": true
}
```

| 필드 | 의미 |
|---|---|
| `predicted` | 모델이 고른 라벨. `가능1`, `시간:10분`처럼 번호나 활용형이 붙을 수 있다. |
| `target` | 오늘 목록과 맞으면 그 한국어 단어. 아니면 `predicted`와 같다. |
| `confidence` | 1위 확률. 0~1. |
| `wordId` | 맞춘 오늘 단어 또는 복습 단어의 아이디. 목록에 없으면 `null`. |
| `correct` | `wordId`가 있으면 `true`. |
| `recorded` | 메인 서버에 정답으로 저장됐으면 `true`. |
| `passed` | 저장에 성공하면 `true`. 저장하지 않았으면 `null`. |

화면 처리:

- `wordId`가 지금 보여주는 단어와 같으면 성공이다.
- `wordId`가 오늘 다른 단어면, 그 단어가 인식된 것이다. 지금 단어의 성공으로 치지 않는다.
- `correct`가 `false`면 오늘 단어로 인식되지 않았다. 소켓은 유지되니 다시 찍으면 된다.
- `correct`가 `true`인데 `recorded`가 `false`면 저장이 실패한 것이다. 성공 처리하지 않고 다시 보낸다.

저장이 된 단어는 다음 `GET /api/progress/daily`의 `isMastered`, `progress`에 반영된다. 소켓 응답만으로 진행도를 조립해도 되고, 화면을 다시 열 때 daily를 다시 받아도 된다.

## 6. error

```json
{
  "type": "error",
  "code": "INVALID_TICKET",
  "message": "티켓이 유효하지 않습니다."
}
```

| 상황 | code | message | 소켓 |
|---|---|---|---|
| 티켓 없음, 서명 불일치, 만료, 클레임 부족 | `INVALID_TICKET` | 티켓이 유효하지 않습니다. | 닫음 |
| 연결된 뒤 `expiresAt`이 지난 다음 메시지 | `INVALID_TICKET` | 티켓이 유효하지 않습니다. | 닫음 |
| `session` 전에 `attempt` | `INVALID_TICKET` | 티켓이 유효하지 않습니다. | 닫음 |
| 좌표 없음, JSON 형식 오류, `type`이 `auth`/`attempt`가 아님 | `NO_LANDMARKS` | 유효한 좌표 시퀀스가 없습니다. | 유지 |
| 모델이 아직 없음 | `NO_LANDMARKS` | 모델이 로드되지 않았습니다. | 유지 |
| 동시 접속이 50을 넘음 | 메시지 없음 | 메시지 없음 | 연결 직후 닫음 |

티켓 실패는 코드가 모두 `INVALID_TICKET`이다. 소켓이 닫히면 `GET /api/progress/daily`로 티켓을 다시 받아 새로 연결한다.

스프링 HTTP 에러는 `{ "message": "..." }`만 있고 `type`, `code`가 없다.
