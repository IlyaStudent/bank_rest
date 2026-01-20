package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CardService {
    CardResponse createCard(@NotNull CreateCardRequest createCardRequest, @NotNull Long userId);

    CardResponse getCardById(@NotNull Long cardId);

    Page<CardResponse> getCardsForUser(@NotNull Long userId, Pageable pageable);

    CardResponse updateCardStatus(@NotNull Long cardId, @NotNull CardStatus cardStatus);

    CardResponse blockCard(@NotNull Long cardId);

    void deleteCard(@NotNull Long cardId);

    BigDecimal getCardBalance(@NotNull Long cardId);
}
