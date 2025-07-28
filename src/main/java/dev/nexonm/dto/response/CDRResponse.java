package dev.nexonm.dto.response;

import dev.nexonm.entity.CallDetailRecord;

import java.time.LocalDateTime;

public record CDRResponse (
        LocalDateTime callTimestamp,
        String numberA,
        String numberB,
        Integer duration,
        String agentName
){
    public static CDRResponse fromEntity(CallDetailRecord entity) {
        return new CDRResponse(
                entity.time,
                entity.numberA,
                entity.numberB,
                entity.duration,
                entity.agent.name
        );
    }
}