package com.docqa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryResponse {

    private Long id;
    private String content;
    private String role;
    private String timestampRef;
}