package org.gh7035.ieumson.domain.progress.presentation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CalendarStreakResponse(
        int year,
        int month,
        List<CalendarDayResponse> days
) {
    public record CalendarDayResponse(
            LocalDate date,
            int intensity
    ) {
    }
}
