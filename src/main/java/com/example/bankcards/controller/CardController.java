package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${end.point.cards}")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(
            @RequestBody @Valid CreateCardRequest createCardRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails

    ) {
        Long userId = userDetails.getId();
        return cardService.createCard(createCardRequest, userId);
    }

    @GetMapping
    public Page<CardResponse> getAllCards(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        return cardService.getCardsForUser(userId, pageable);
    }

    @GetMapping("${end.point.id}")
    public CardResponse getCardInfo(
            @PathVariable(name = "id") Long cardId
    ) {
        return cardService.getCardById(cardId);
    }

    @PutMapping("${end.point.id}")
    public CardResponse updateCard(
            @RequestBody @Valid CardUpdateRequest cardUpdateRequest,
            @PathVariable(name = "id") Long cardId
    ) {
        return cardService.updateCard(cardId, cardUpdateRequest);
    }

    @DeleteMapping("${end.point.id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(
            @PathVariable(name = "id") Long cardId
    ) {
        cardService.deleteCard(cardId);
    }

    @PutMapping("${end.point.block}")
    public CardResponse blockCard(
            @PathVariable(name = "id") Long cardId
    ) {
        return cardService.blockCard(cardId);
    }
}
