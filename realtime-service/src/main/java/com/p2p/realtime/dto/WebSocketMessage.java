package com.p2p.realtime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage<T> {

    @NotBlank(message = "Message type cannot be empty")
    private String type;

    @NotNull(message = "Payload cannot be null")
    private T payload;

    @NotNull(message = "Timestamp cannot be null")
    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

    private String sessionId;

    public static <T> WebSocketMessage<T> create(String type, T payload) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .payload(payload)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public enum MessageType {
        LOCATION_UPDATE,
        TRIP_UPDATE,
        SAFETY_ALERT,
        EMERGENCY,
        NEARBY_EMERGENCY,
        NOTIFICATION,
        ERROR
    }
}