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
import com.example.bankcards.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private CardServiceImpl cardService;

    private CreateCardRequest createCardRequest;
    private CardUpdateRequest cardUpdateRequest;
    private CardResponse cardResponse;
    private Card card;
    private User owner;
    private Long cardId;
    private Long userId;
    private String cardNumber;
    private String encryptedCardNumber;
    private String maskedCardNumber;
    private String holderName;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        cardId = 1L;
        userId = 1L;
        cardNumber = "1111 2222 3333 4444";
        encryptedCardNumber = "encrypted_card_number";
        maskedCardNumber = "**** **** **** 4444";
        holderName = "IVAN IVANOV";

        pageable = PageRequest.of(0, 10);

        owner = User.builder()
                .id(userId)
                .username("test")
                .email("test@example.com")
                .password("password")
                .build();

        createCardRequest = CreateCardRequest.builder()
                .cardNumber(cardNumber)
                .holderName(holderName)
                .expiryDate("12/30")
                .cvv("777")
                .build();

        cardUpdateRequest = CardUpdateRequest.builder()
                .status("ACTIVE")
                .build();

        card = Card.builder()
                .id(cardId)
                .cardNumber(encryptedCardNumber)
                .owner(owner)
                .holderName(holderName)
                .expiryDate(LocalDate.of(2030, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        cardResponse = CardResponse.builder()
                .id(cardId)
                .maskedCardNumber(maskedCardNumber)
                .holderName(holderName)
                .expiryDate("12/30")
                .status(CardStatus.ACTIVE.name())
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    @Nested
    @DisplayName("createCard")
    class CreateCard {

        @Test
        @DisplayName("Should create card successfully")
        void shouldCreateCardSuccessfully() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
            when(encryptionUtil.encrypt(cardNumber)).thenReturn(encryptedCardNumber);
            when(cardRepository.save(any(Card.class))).thenReturn(card);
            when(cardMapper.toResponse(card)).thenReturn(cardResponse);

            CardResponse result = cardService.createCard(createCardRequest, userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(cardId);
            assertThat(result.getHolderName()).isEqualTo(holderName);
            assertThat(result.getMaskedCardNumber()).isEqualTo(maskedCardNumber);

            verify(userRepository).findById(userId);
            verify(encryptionUtil).encrypt(cardNumber);
            verify(cardRepository).save(any(Card.class));
            verify(cardMapper).toResponse(card);
        }

        @Test
        @DisplayName("Should encrypt card number before saving")
        void shouldEncryptCardNumberBeforeSaving() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
            when(encryptionUtil.encrypt(cardNumber)).thenReturn(encryptedCardNumber);
            when(cardRepository.save(any(Card.class))).thenReturn(card);
            when(cardMapper.toResponse(card)).thenReturn(cardResponse);

            cardService.createCard(createCardRequest, userId);

            verify(encryptionUtil).encrypt(cardNumber);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createCard(createCardRequest, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository).findById(userId);
            verify(cardRepository, never()).save(any(Card.class));
        }

        @Test
        @DisplayName("Should throw exception when expiry date format invalid")
        void shouldThrowExceptionWhenExpiryDateFormatInvalid() {
            CreateCardRequest invalidRequest = CreateCardRequest.builder()
                    .cardNumber(cardNumber)
                    .holderName(holderName)
                    .expiryDate("invalid-date")
                    .cvv("777")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
            when(encryptionUtil.encrypt(cardNumber)).thenReturn(encryptedCardNumber);

            assertThatThrownBy(() -> cardService.createCard(invalidRequest, userId))
                    .isInstanceOf(BusinessException.class);

            verify(cardRepository, never()).save(any(Card.class));
        }

    }

    @Nested
    @DisplayName("getCardById")
    class GetCardById {

        @Test
        @DisplayName("Should return card when found")
        void shouldReturnCardWhenFound() {
            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
            when(cardMapper.toResponse(card)).thenReturn(cardResponse);

            CardResponse result = cardService.getCardById(cardId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(cardId);
            assertThat(result.getHolderName()).isEqualTo(holderName);
            assertThat(result.getStatus()).isEqualTo(CardStatus.ACTIVE.name());

            verify(cardRepository).findById(cardId);
            verify(cardMapper).toResponse(card);
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.getCardById(cardId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository).findById(cardId);
            verify(cardMapper, never()).toResponse(any(Card.class));
        }

    }

    @Nested
    @DisplayName("getCardsForUser")
    class GetCardsForUser {

        @Test
        @DisplayName("Should return page with cards")
        void shouldReturnPageWithCards() {
            List<Card> cards = List.of(card);
            Page<Card> cardPage = new PageImpl<>(cards, pageable, cards.size());

            when(userRepository.existsById(userId)).thenReturn(true);
            when(cardRepository.findByOwnerId(userId, pageable)).thenReturn(cardPage);
            when(cardMapper.toResponse(card)).thenReturn(cardResponse);

            Page<CardResponse> result = cardService.getCardsForUser(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getHolderName()).isEqualTo(holderName);

            verify(userRepository).existsById(userId);
            verify(cardRepository).findByOwnerId(userId, pageable);
            verify(cardMapper).toResponse(card);
        }

        @Test
        @DisplayName("Should return empty page with no cards")
        void shouldReturnEmptyPageWithNoCards() {
            Page<Card> emptyPage = Page.empty(pageable);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(cardRepository.findByOwnerId(userId, pageable)).thenReturn(emptyPage);

            Page<CardResponse> result = cardService.getCardsForUser(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.hasContent()).isFalse();

            verify(userRepository).existsById(userId);
            verify(cardRepository).findByOwnerId(userId, pageable);
            verify(cardMapper, never()).toResponse(any(Card.class));
        }

        @Test
        @DisplayName("Should return correct page metadata")
        void shouldReturnCorrectPageMetadata() {
            Pageable customPageable = PageRequest.of(1, 5);
            List<Card> cards = List.of(card);
            Page<Card> cardPage = new PageImpl<>(cards, customPageable, 11);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(cardRepository.findByOwnerId(userId, customPageable)).thenReturn(cardPage);
            when(cardMapper.toResponse(any(Card.class))).thenReturn(cardResponse);

            Page<CardResponse> result = cardService.getCardsForUser(userId, customPageable);

            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(11);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> cardService.getCardsForUser(userId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository).existsById(userId);
            verify(cardRepository, never()).findByOwnerId(any(), any());
        }

    }

    @Nested
    @DisplayName("updateCard")
    class UpdateCard {

        @Test
        @DisplayName("Should update card successfully with new status")
        void shouldUpdateCardSuccessfullyWithNewStatus() {
            cardUpdateRequest.setStatus("BLOCKED");
            CardResponse blockedResponse = CardResponse.builder()
                    .id(cardId)
                    .maskedCardNumber(maskedCardNumber)
                    .holderName(holderName)
                    .status(CardStatus.BLOCKED.name())
                    .build();

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
            when(cardRepository.save(card)).thenReturn(card);
            when(cardMapper.toResponse(card)).thenReturn(blockedResponse);

            CardResponse result = cardService.updateCard(cardId, cardUpdateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CardStatus.BLOCKED.name());
            assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);

            verify(cardRepository).findById(cardId);
            verify(cardRepository).save(card);
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.updateCard(cardId, cardUpdateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository).findById(cardId);
            verify(cardRepository, never()).save(any(Card.class));
        }

        @Test
        @DisplayName("Should throw exception when card status invalid")
        void shouldThrowExceptionWhenCardStatusInvalid() {
            cardUpdateRequest.setStatus("INVALID_STATUS");

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> cardService.updateCard(cardId, cardUpdateRequest))
                    .isInstanceOf(BusinessException.class);

            verify(cardRepository).findById(cardId);
            verify(cardRepository, never()).save(any(Card.class));
        }
    }

    @Nested
    @DisplayName("blockCard")
    class BlockCard {

        @Test
        @DisplayName("Should block card successfully")
        void shouldBlockCardSuccessfully() {
            CardResponse blockedResponse = CardResponse.builder()
                    .id(cardId)
                    .maskedCardNumber(maskedCardNumber)
                    .holderName(holderName)
                    .status(CardStatus.BLOCKED.name())
                    .build();

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
            when(cardRepository.save(card)).thenReturn(card);
            when(cardMapper.toResponse(card)).thenReturn(blockedResponse);

            CardResponse result = cardService.blockCard(cardId);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CardStatus.BLOCKED.name());
            assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);

            verify(cardRepository).findById(cardId);
            verify(cardRepository).save(card);
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.blockCard(cardId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository).findById(cardId);
            verify(cardRepository, never()).save(any(Card.class));
        }

    }

    @Nested
    @DisplayName("deleteCard")
    class DeleteCard {

        @Test
        @DisplayName("Should delete card successfully")
        void shouldDeleteCardSuccessfully() {
            when(cardRepository.existsById(cardId)).thenReturn(true);

            cardService.deleteCard(cardId);

            verify(cardRepository).existsById(cardId);
            verify(cardRepository).deleteById(cardId);
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            when(cardRepository.existsById(cardId)).thenReturn(false);

            assertThatThrownBy(() -> cardService.deleteCard(cardId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository).existsById(cardId);
            verify(cardRepository, never()).deleteById(any());
        }
    }

}
