package com.docqa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.stereotype.Service;

import com.docqa.model.Timestamp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final OpenAiChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================
    // SUMMARIZE CONTENT
    // =========================
    public String summarizeContent(String content, String fileType) {

        String prompt = String.format("""
                You are an expert AI assistant.

                Summarize the following %s content clearly and concisely.

                Requirements:
                - Use bullet points
                - Mention important topics
                - Keep summary under 300 words
                - Make response easy to understand

                Content:
                %s
                """, fileType, truncate(content, 8000));

        try {

            return chatClient.call(prompt);

        } catch (Exception e) {

            log.error("Failed to summarize content", e);

            return "Failed to summarize content.";
        }
    }

    // =========================
    // ASK QUESTION
    // =========================
    public com.docqa.dto.ChatResponse askQuestion(
            String question,
            String documentContent,
            List<Timestamp> timestamps,
            String docType) {

        try {

            List<Message> messages = new ArrayList<>();

            String systemPrompt =
                    buildSystemPrompt(documentContent, timestamps, docType);

            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(question));

            Prompt prompt = new Prompt(messages);

            String answer = chatClient.call(prompt)
                    .getResult()
                    .getOutput()
                    .getContent();

            // Find relevant timestamps
            Double timestampRef = null;

            List<com.docqa.dto.ChatResponse.TimestampDto>
                    relevantTimestamps = new ArrayList<>();

            if (!timestamps.isEmpty()) {

                relevantTimestamps =
                        findRelevantTimestamps(question, timestamps);

                if (!relevantTimestamps.isEmpty()) {

                    timestampRef =
                            relevantTimestamps.get(0).getStartTime();
                }
            }

            return com.docqa.dto.ChatResponse.builder()
                    .answer(answer)
                    .timestampRef(timestampRef)
                    .relevantTimestamps(relevantTimestamps)
                    .build();

        } catch (Exception e) {

            log.error("Failed to answer question", e);

            return com.docqa.dto.ChatResponse.builder()
                    .answer("Failed to process your question.")
                    .build();
        }
    }

    // =========================
    // EXTRACT TIMESTAMPS
    // =========================
    public List<Timestamp> extractTimestamps(String transcription) {

        String prompt = String.format("""
                Analyze this audio/video transcription.

                Extract important timestamps and topics.

                Return ONLY valid JSON array.

                Format:
                [
                  {
                    "startTime": 0,
                    "endTime": 30,
                    "text": "Introduction text",
                    "topic": "Introduction"
                  }
                ]

                Transcription:
                %s
                """, truncate(transcription, 6000));

        try {

            String response = chatClient.call(prompt);

            return parseTimestampsFromJson(response);

        } catch (Exception e) {

            log.error("Failed to extract timestamps", e);

            return new ArrayList<>();
        }
    }

    // =========================
    // BUILD SYSTEM PROMPT
    // =========================
    private String buildSystemPrompt(String content,
                                     List<Timestamp> timestamps,
                                     String docType) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
                You are an intelligent AI assistant.

                Your job is to answer user questions ONLY
                from the uploaded document/audio/video content.

                Rules:
                1. Always answer from the provided content.
                2. If answer exists, answer confidently.
                3. Never say content is missing if content exists.
                4. If user asks for summary, summarize clearly.
                5. For audio/video mention timestamps when available.
                6. If answer truly does not exist say:
                   "This information is not available in the uploaded content."

                Uploaded Content:
                """);

        sb.append("\n");
        sb.append(truncate(content, 7000));

        // Add timestamps
        if (!timestamps.isEmpty()) {

            sb.append("\n\nAvailable Timestamps:\n");

            timestamps.forEach(t ->
                    sb.append(String.format(
                            "[%.1fs - %.1fs] %s : %s\n",
                            t.getStartTime(),
                            t.getEndTime(),
                            t.getTopic(),
                            t.getText()
                    ))
            );
        }

        return sb.toString();
    }

    // =========================
    // FIND RELEVANT TIMESTAMPS
    // =========================
    private List<com.docqa.dto.ChatResponse.TimestampDto>
    findRelevantTimestamps(
            String question,
            List<Timestamp> timestamps
    ) {

        String lowerQuestion = question.toLowerCase();

        return timestamps.stream()

                .filter(t -> {

                    boolean topicMatch =
                            t.getTopic() != null &&
                                    lowerQuestion.contains(
                                            t.getTopic().toLowerCase()
                                    );

                    boolean textMatch =
                            t.getText() != null &&
                                    t.getText()
                                            .toLowerCase()
                                            .contains(
                                                    lowerQuestion.substring(
                                                            0,
                                                            Math.min(
                                                                    lowerQuestion.length(),
                                                                    20
                                                            )
                                                    )
                                            );

                    return topicMatch || textMatch;
                })

                .limit(3)

                .map(t -> com.docqa.dto.ChatResponse.TimestampDto
                        .builder()
                        .startTime(t.getStartTime())
                        .endTime(t.getEndTime())
                        .text(t.getText())
                        .topic(t.getTopic())
                        .build())

                .toList();
    }

    // =========================
    // PARSE TIMESTAMPS JSON
    // =========================
    private List<Timestamp> parseTimestampsFromJson(String json) {

        try {

            json = json
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            List<TimestampDto> dtos =
                    objectMapper.readValue(
                            json,
                            new TypeReference<List<TimestampDto>>() {}
                    );

            List<Timestamp> timestamps = new ArrayList<>();

            for (TimestampDto dto : dtos) {

                Timestamp timestamp = new Timestamp();

                timestamp.setStartTime(dto.getStartTime());
                timestamp.setEndTime(dto.getEndTime());
                timestamp.setText(dto.getText());
                timestamp.setTopic(dto.getTopic());

                timestamps.add(timestamp);
            }

            return timestamps;

        } catch (Exception e) {

            log.error("Failed to parse timestamps JSON", e);

            return new ArrayList<>();
        }
    }

    // =========================
    // TRUNCATE TEXT
    // =========================
    private String truncate(String text, int maxLength) {

        if (text == null) {
            return "";
        }

        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }

    // =========================
    // DTO FOR JSON PARSING
    // =========================
    @lombok.Data
    private static class TimestampDto {

        private Double startTime;
        private Double endTime;
        private String text;
        private String topic;
    }
}