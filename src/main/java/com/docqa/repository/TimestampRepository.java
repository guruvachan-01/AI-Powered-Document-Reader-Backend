package com.docqa.repository;

import com.docqa.model.Document;
import com.docqa.model.Timestamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimestampRepository extends JpaRepository<Timestamp, Long> {
    List<Timestamp> findByDocumentOrderByStartTimeAsc(Document document);
    List<Timestamp> findByDocumentAndTopicContainingIgnoreCase(Document document, String topic);
}
