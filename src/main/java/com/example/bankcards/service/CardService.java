package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CardService {
    CardResponse createCard(@NotNull CreateCardRequest createCardRequest, @NotNull Long userId);

    CardResponse getCardById(@NotNull Long userId, @NotNull Long cardId);

    Page<CardResponse> getCardsForUser(@NotNull Long userId, Pageable pageable);

    CardResponse updateCard(@NotNull Long userId, @NotNull Long cardId, @NotNull CardUpdateRequest cardUpdateRequest);

    CardResponse blockCard(@NotNull Long userId, @NotNull Long cardId);

    void deleteCard(@NotNull Long userId, @NotNull Long cardId);
}
