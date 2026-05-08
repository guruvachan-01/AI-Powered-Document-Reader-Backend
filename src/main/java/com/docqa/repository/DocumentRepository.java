package com.docqa.repository;

import com.docqa.model.Document;
import com.docqa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserOrderByUploadedAtDesc(User user);
    Optional<Document> findByIdAndUser(Long id, User user);
    List<Document> findByUserAndFileType(User user, Document.FileType fileType);

    @Query("SELECT d FROM Document d WHERE d.user = :user AND SIZE(d.messages) > 0")
    List<Document> findByUserWithMessages(User user);
}
