package org.gh7035.ieumson.infrastructure.ticket;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "ai")
public record DetectTicketProperties(
        String ticketSecret,
        Long ticketExpiry
) {
    public static final int MIN_SECRET_BYTES = 32;

    public DetectTicketProperties {
        if (ticketSecret == null || ticketSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("TICKET_SECRET은 32바이트 이상이어야 합니다.");
        }
        if (ticketExpiry == null || ticketExpiry <= 0) {
            ticketExpiry = 14_400_000L;
        }
    }
}
