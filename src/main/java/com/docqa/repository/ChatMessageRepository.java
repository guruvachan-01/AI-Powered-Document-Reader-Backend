package com.docqa.repository;

import com.docqa.model.ChatMessage;
import com.docqa.model.Document;
import com.docqa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByDocumentOrderByCreatedAtAsc(Document document);
    List<ChatMessage> findByUserOrderByCreatedAtDesc(User user);
    void deleteByDocument(Document document);
}
