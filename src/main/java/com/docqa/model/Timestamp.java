package com.docqa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "timestamps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Timestamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time")
    private Double startTime;

    @Column(name = "end_time")
    private Double endTime;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
}
