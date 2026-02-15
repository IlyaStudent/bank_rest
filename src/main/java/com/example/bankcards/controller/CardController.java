package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cards", description = "Bank card management")
@RestController
@RequiredArgsConstructor
@RequestMapping("${end.point.cards}")
public class CardController {

    private final CardService cardService;

    @Operation(summary = "Create new card", description = "Creates a new bank card for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Card created successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(
            @RequestBody @Valid CreateCardRequest createCardRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        return cardService.createCard(createCardRequest, userId);
    }

    @Operation(summary = "Get all cards", description = "Returns paginated list of cards for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Cards retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping
    public Page<CardResponse> getAllCards(
            Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        return cardService.getCardsForUser(userId, pageable);
    }

    @Operation(summary = "Get card by ID", description = "Returns card details by card ID")
    @ApiResponse(responseCode = "200", description = "Card found")
    @ApiResponse(responseCode = "404", description = "Card not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("${end.point.id}")
    public CardResponse getCardById(
            @Parameter(description = "Card ID") @PathVariable(name = "id") Long cardId
    ) {
        return cardService.getCardById(cardId);
    }

    @Operation(summary = "Update card status", description = "Updates the status of a card (ACTIVE, BLOCKED, EXPIRED)")
    @ApiResponse(responseCode = "200", description = "Card updated successfully")
    @ApiResponse(responseCode = "404", description = "Card not found")
    @ApiResponse(responseCode = "422", description = "Invalid card status")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("${end.point.id}")
    public CardResponse updateCard(
            @RequestBody @Valid CardUpdateRequest cardUpdateRequest,
            @Parameter(description = "Card ID") @PathVariable(name = "id") Long cardId
    ) {
        return cardService.updateCard(cardId, cardUpdateRequest);
    }

    @Operation(summary = "Delete card", description = "Deletes a card by ID (admin only)")
    @ApiResponse(responseCode = "204", description = "Card deleted successfully")
    @ApiResponse(responseCode = "404", description = "Card not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @DeleteMapping("${end.point.id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(
            @Parameter(description = "Card ID") @PathVariable(name = "id") Long cardId
    ) {
        cardService.deleteCard(cardId);
    }

    @Operation(summary = "Block card", description = "Blocks a card by ID")
    @ApiResponse(responseCode = "200", description = "Card blocked successfully")
    @ApiResponse(responseCode = "404", description = "Card not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("${end.point.block}")
    public CardResponse blockCard(
            @Parameter(description = "Card ID") @PathVariable(name = "id") Long cardId
    ) {
        return cardService.blockCard(cardId);
    }
}
