package com.example.bankcards.service.impl;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private static final DateTimeFormatter EXPIRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;
    private final EncryptionUtil encryptionUtil;

    @Override
    public CardResponse createCard(CreateCardRequest createCardRequest, Long userId) {
        log.debug("Creating card for user id={}", userId);

        User user = findUserById(userId);
        Card card = buildCard(createCardRequest, user);
        card = cardRepository.save(card);

        log.info("Card created: id={}, userId={}, holder='{}'", card.getId(), userId, card.getHolderName());

        return cardMapper.toResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        return cardMapper.toResponse(findCardById(cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsForUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.user(userId);
        }

        return cardRepository
                .findByOwnerId(userId, pageable)
                .map(cardMapper::toResponse);
    }

    @Override
    public CardResponse updateCard(Long cardId, CardUpdateRequest cardUpdateRequest) {
        Card card = findCardById(cardId);
        return changeCardStatus(card, cardUpdateRequest.getStatus());
    }

    @Override
    public CardResponse blockCard(Long cardId) {
        log.debug("Blocking card id={}", cardId);

        Card card = findCardById(cardId);
        CardResponse response = changeCardStatus(card, CardStatus.BLOCKED.name());

        log.info("Card blocked: id={}", cardId);

        return response;
    }

    @Override
    public void deleteCard(Long cardId) {
        log.debug("Deleting card id={}", cardId);

        if (!cardRepository.existsById(cardId)) {
            throw ResourceNotFoundException.card(cardId);
        }
        cardRepository.deleteById(cardId);

        log.info("Card deleted: id={}", cardId);
    }

    // --- Lookup --- //

    private Card findCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.card(cardId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));
    }

    // --- Card building --- //

    private Card buildCard(CreateCardRequest request, User owner) {
        return Card.builder()
                .cardNumber(encryptionUtil.encrypt(request.getCardNumber()))
                .owner(owner)
                .holderName(request.getHolderName())
                .expiryDate(parseExpiryDate(request.getExpiryDate()))
                .build();
    }

    private LocalDate parseExpiryDate(String expiryDate) {
        return YearMonth.parse(expiryDate, EXPIRY_DATE_FORMATTER).atEndOfMonth();
    }

    // --- Status management --- //

    private CardResponse changeCardStatus(Card card, String requestedStatus) {
        CardStatus newStatus = parseCardStatus(requestedStatus);
        CardStatus previousStatus = card.getStatus();

        card.setStatus(newStatus);
        card = cardRepository.save(card);

        log.debug("Card status updated: id={}, {} -> {}", card.getId(), previousStatus, newStatus);

        return cardMapper.toResponse(card);
    }

    private CardStatus parseCardStatus(String status) {
        try {
            return CardStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalidCardStatus(status);
        }
    }
}
