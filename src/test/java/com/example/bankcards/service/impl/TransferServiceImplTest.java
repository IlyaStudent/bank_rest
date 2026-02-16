package com.example.bankcards.service.impl;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.*;
import com.example.bankcards.event.TransferEvent;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.service.KafkaProducerService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private TransferServiceImpl transferService;

    private TransferRequest transferRequest;
    private TransferResponse transferResponse;
    private Transfer transfer;
    private Card sourceCard;
    private Card destinationCard;
    private Long sourceCardId;
    private Long destinationCardId;
    private Long userId;
    private BigDecimal transferAmount;
    private BigDecimal sourceCardBalance;
    private BigDecimal destinationCardBalance;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sourceCardId = 1L;
        destinationCardId = 2L;
        userId = 1L;
        transferAmount = new BigDecimal("100.00");
        sourceCardBalance = new BigDecimal("500.00");
        destinationCardBalance = new BigDecimal("200.00");

        pageable = PageRequest.of(0, 10);

        User owner = User.builder()
                .id(userId)
                .username("test")
                .email("test@example.com")
                .password("password")
                .build();

        sourceCard = Card.builder()
                .id(sourceCardId)
                .cardNumber("1111 2222 3333 4444")
                .owner(owner)
                .holderName("IVAN IVANOV")
                .expiryDate(LocalDate.of(2030, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(sourceCardBalance)
                .build();

        destinationCard = Card.builder()
                .id(destinationCardId)
                .cardNumber("5555 6666 7777 8888")
                .owner(owner)
                .holderName("PETR PETROV")
                .expiryDate(LocalDate.of(2030, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(destinationCardBalance)
                .build();

        transferRequest = TransferRequest.builder()
                .sourceCardId(sourceCardId)
                .destinationCardId(destinationCardId)
                .amount(transferAmount)
                .description("Test transfer")
                .build();

        transfer = Transfer.builder()
                .id(1L)
                .sourceCard(sourceCard)
                .destinationCard(destinationCard)
                .amount(transferAmount)
                .description("Test transfer")
                .status(TransferStatus.SUCCESS)
                .timestamp(Instant.now())
                .build();

        transferResponse = TransferResponse.builder()
                .id(1L)
                .sourceCardMasked("**** **** **** 4444")
                .destinationCardMasked("**** **** **** 8888")
                .amount(transferAmount)
                .status(TransferStatus.SUCCESS.name())
                .timestamp(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("transferMoney")
    class TransferMoney {

        @Test
        @DisplayName("Should transfer money successfully")
        void shouldTransferMoneySuccessfully() {
            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
            when(cardRepository.findById(destinationCardId)).thenReturn(Optional.of(destinationCard));
            when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
            when(transferMapper.toResponse(transfer)).thenReturn(transferResponse);

            TransferResponse result = transferService.transferMoney(transferRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(transferAmount);
            assertThat(result.getStatus()).isEqualTo(TransferStatus.SUCCESS.name());

            assertThat(sourceCard.getBalance()).isEqualTo(sourceCardBalance.subtract(transferAmount));
            assertThat(destinationCard.getBalance()).isEqualTo(destinationCardBalance.add(transferAmount));

            verify(cardRepository).findById(sourceCardId);
            verify(cardRepository).findById(destinationCardId);
            verify(transferRepository).save(any(Transfer.class));
            verify(transferMapper).toResponse(transfer);
            verify(kafkaProducerService).sendTransferEvent(any(TransferEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when transfer amount is not positive")
        void shouldThrowExceptionWhenTransferAmountIsNotPositive() {
            transferRequest.setAmount(new BigDecimal("-50.00"));

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(cardRepository, never()).findById(any());
            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when transfer amount is zero")
        void shouldThrowExceptionWhenTransferAmountIsZero() {
            transferRequest.setAmount(BigDecimal.ZERO);

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(cardRepository, never()).findById(any());
            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when source card balance less than transfer amount")
        void shouldThrowExceptionWhenSourceCardBalanceLessThanTransferAmount() {
            transferRequest.setAmount(new BigDecimal("1000.00"));

            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
            when(cardRepository.findById(destinationCardId)).thenReturn(Optional.of(destinationCard));

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when source card equals to destination card")
        void shouldThrowExceptionWhenSourceCardEqualsToDestinationCard() {
            transferRequest.setSourceCardId(sourceCardId);
            transferRequest.setDestinationCardId(sourceCardId);

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(cardRepository, never()).findById(any());
            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when source card not found")
        void shouldThrowExceptionWhenSourceCardNotFound() {
            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository).findById(sourceCardId);
            verify(cardRepository, never()).findById(destinationCardId);
            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when source card blocked")
        void shouldThrowExceptionWhenSourceCardBlocked() {
            Card blockedSourceCard = Card.builder()
                    .id(sourceCardId)
                    .cardNumber("1111 2222 3333 4444")
                    .holderName("IVAN IVANOV")
                    .expiryDate(LocalDate.of(2030, 12, 31))
                    .status(CardStatus.BLOCKED)
                    .balance(sourceCardBalance)
                    .build();

            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(blockedSourceCard));
            when(cardRepository.findById(destinationCardId)).thenReturn(Optional.of(destinationCard));

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when source card expired")
        void shouldThrowExceptionWhenSourceCardExpired() {
            Card expiredSourceCard = Card.builder()
                    .id(sourceCardId)
                    .cardNumber("1111 2222 3333 4444")
                    .holderName("IVAN IVANOV")
                    .expiryDate(LocalDate.of(2030, 12, 31))
                    .status(CardStatus.EXPIRED)
                    .balance(sourceCardBalance)
                    .build();

            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(expiredSourceCard));
            when(cardRepository.findById(destinationCardId)).thenReturn(Optional.of(destinationCard));

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when destination card not found")
        void shouldThrowExceptionWhenDestinationCardNotFound() {
            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
            when(cardRepository.findById(destinationCardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository).findById(sourceCardId);
            verify(cardRepository).findById(destinationCardId);
            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when destination card blocked")
        void shouldThrowExceptionWhenDestinationCardBlocked() {
            Card blockedDestinationCard = Card.builder()
                    .id(destinationCardId)
                    .cardNumber("5555 6666 7777 8888")
                    .holderName("PETR PETROV")
                    .expiryDate(LocalDate.of(2030, 12, 31))
                    .status(CardStatus.BLOCKED)
                    .balance(destinationCardBalance)
                    .build();

            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
            when(cardRepository.findById(destinationCardId)).thenReturn(Optional.of(blockedDestinationCard));

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when destination card expired")
        void shouldThrowExceptionWhenDestinationCardExpired() {
            Card expiredDestinationCard = Card.builder()
                    .id(destinationCardId)
                    .cardNumber("5555 6666 7777 8888")
                    .holderName("PETR PETROV")
                    .expiryDate(LocalDate.of(2030, 12, 31))
                    .status(CardStatus.EXPIRED)
                    .balance(destinationCardBalance)
                    .build();

            when(cardRepository.findById(sourceCardId)).thenReturn(Optional.of(sourceCard));
            when(cardRepository.findById(destinationCardId)).thenReturn(Optional.of(expiredDestinationCard));

            assertThatThrownBy(() -> transferService.transferMoney(transferRequest))
                    .isInstanceOf(BusinessException.class);

            verify(transferRepository, never()).save(any(Transfer.class));
            verify(kafkaProducerService, never()).sendTransferEvent(any());
        }

    }

    @Nested
    @DisplayName("getTransferHistory")
    class GetTransferHistory {

        @Test
        @DisplayName("Should return page with transfer history")
        void shouldReturnPageWithTransferHistory() {
            List<Transfer> transfers = List.of(transfer);
            Page<Transfer> transferPage = new PageImpl<>(transfers, pageable, transfers.size());

            when(transferRepository.findBySourceCardOwnerIdOrDestinationCardOwnerId(userId, userId, pageable))
                    .thenReturn(transferPage);
            when(transferMapper.toResponse(transfer)).thenReturn(transferResponse);

            Page<TransferResponse> result = transferService.getTransferHistory(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getAmount()).isEqualTo(transferAmount);

            verify(transferRepository).findBySourceCardOwnerIdOrDestinationCardOwnerId(userId, userId, pageable);
            verify(transferMapper).toResponse(transfer);
        }

        @Test
        @DisplayName("Should return correct page metadata")
        void shouldReturnCorrectPageMetadata() {
            Pageable customPageable = PageRequest.of(1, 5);
            List<Transfer> transfers = List.of(transfer);
            Page<Transfer> transferPage = new PageImpl<>(transfers, customPageable, 11);

            when(transferRepository.findBySourceCardOwnerIdOrDestinationCardOwnerId(userId, userId, customPageable))
                    .thenReturn(transferPage);
            when(transferMapper.toResponse(any(Transfer.class))).thenReturn(transferResponse);

            Page<TransferResponse> result = transferService.getTransferHistory(userId, customPageable);

            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(11);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("Should return page with no transfer history")
        void shouldReturnPageWithNoTransferHistory() {
            Page<Transfer> emptyPage = Page.empty(pageable);

            when(transferRepository.findBySourceCardOwnerIdOrDestinationCardOwnerId(userId, userId, pageable))
                    .thenReturn(emptyPage);

            Page<TransferResponse> result = transferService.getTransferHistory(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.hasContent()).isFalse();

            verify(transferRepository).findBySourceCardOwnerIdOrDestinationCardOwnerId(userId, userId, pageable);
            verify(transferMapper, never()).toResponse(any(Transfer.class));
        }
    }
}
