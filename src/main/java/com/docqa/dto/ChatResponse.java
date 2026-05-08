package com.docqa.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatResponse {
    private String answer;
    private Double timestampRef;
    private List<TimestampDto> relevantTimestamps;
    private Long messageId;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TimestampDto {
        private Double startTime;
        private Double endTime;
        private String text;
        private String topic;
    }
}
