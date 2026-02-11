package com.weekly.repository;

import com.weekly.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdAndUsedFalseOrderByCreatedAtAsc(Long userId);

    void deleteByUserIdAndUsedFalse(Long userId);
}
