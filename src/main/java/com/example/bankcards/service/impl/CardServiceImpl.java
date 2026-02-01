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
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;
    private final EncryptionUtil encryptionUtil;

    @Override
    public CardResponse createCard(CreateCardRequest createCardRequest, Long userId) {
        log.debug("Creating card for user id={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId));

        String encryptedCardNumber = encryptionUtil.encrypt(createCardRequest.getCardNumber());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        YearMonth yearMonth = YearMonth.parse(createCardRequest.getExpiryDate(), formatter);
        LocalDate expiryDate = yearMonth.atEndOfMonth();

        Card card = Card.builder()
                .cardNumber(encryptedCardNumber)
                .owner(user)
                .holderName(createCardRequest.getHolderName())
                .expiryDate(expiryDate)
                .build();

        card = cardRepository.save(card);

        log.info("Card created: id={}, userId={}, holder='{}'", card.getId(), userId, card.getHolderName());

        return cardMapper.toResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.card(cardId));

        return cardMapper.toResponse(card);
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
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.card(cardId));

        updateCardStatus(card, cardUpdateRequest.getStatus());

        return cardMapper.toResponse(card);
    }

    @Override
    public CardResponse blockCard(Long cardId) {
        log.debug("Blocking card id={}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.card(cardId));

        CardResponse response = updateCardStatus(card, CardStatus.BLOCKED.name());

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

    private CardResponse updateCardStatus(Card card, String requestedCardStatus) {
        CardStatus cardStatus;
        try {
            cardStatus = CardStatus.valueOf(requestedCardStatus);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalidCardStatus(requestedCardStatus);
        }

        CardStatus previousStatus = card.getStatus();
        card.setStatus(cardStatus);
        card = cardRepository.save(card);

        log.debug("Card status updated: id={}, previousStatus={}, newStatus={}", card.getId(), previousStatus, cardStatus);

        return cardMapper.toResponse(card);
    }
}
