package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Page<Card> findByOwnerId(
            Long ownerId,
            Pageable pageable
    );

    Optional<Card> findByOwnerIdAndId(
            Long ownerId,
            Long cardId
    );

    Page<Card> findByStatus(
            @Param("status") CardStatus status,
            Pageable pageable
    );

    Page<Card> findByOwnerIdAndStatus(
            Long ownerId,
            CardStatus status,
            Pageable pageable
    );

    Page<Card> findByOwnerIdAndBalanceGreaterThen(
            Long ownerId,
            BigDecimal balance,
            Pageable pageable
    );
}
