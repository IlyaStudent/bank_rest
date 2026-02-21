package com.example.bankcards.service.impl;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.event.TransferEvent;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.service.KafkaProducerService;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final TransferMapper transferMapper;
    private final KafkaProducerService kafkaProducerService;
    private final EncryptionUtil encryptionUtil;

    @Override
    public TransferResponse transferMoney(TransferRequest transferRequest, Long userId) {
        log.debug("Transfer request: sourceCardId={}, destinationCardId={}, amount={}",
                transferRequest.getSourceCardId(), transferRequest.getDestinationCardId(), transferRequest.getAmount());

        validateTransferRequest(transferRequest);

        Card sourceCard = findCardById(transferRequest.getSourceCardId());
        Card destinationCard = findCardById(transferRequest.getDestinationCardId());

        validateCardOwnership(sourceCard, userId);
        validateCardForTransfer(sourceCard);
        validateCardForTransfer(destinationCard);
        validateSufficientFunds(sourceCard, transferRequest.getAmount());

        executeBalanceTransfer(sourceCard, destinationCard, transferRequest.getAmount());

        Transfer transfer = createTransfer(transferRequest, sourceCard, destinationCard);
        publishTransferEvent(transfer, sourceCard, destinationCard);

        log.info("Transfer completed: id={}, sourceCardId={}, destinationCardId={}, amount={}",
                transfer.getId(), sourceCard.getId(), destinationCard.getId(), transferRequest.getAmount());

        return transferMapper.toResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponse> getTransferHistory(Long userId, Pageable pageable) {
        return transferRepository
                .findBySourceCardOwnerIdOrDestinationCardOwnerId(userId, userId, pageable)
                .map(transferMapper::toResponse);
    }

    // --- Validation --- //

    private void validateTransferRequest(TransferRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalidTransferAmount();
        }
        if (request.getSourceCardId().equals(request.getDestinationCardId())) {
            throw BusinessException.sameCardTransfer();
        }
    }

    private void validateCardForTransfer(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw BusinessException.cardBlocked(card.getId());
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw BusinessException.cardExpired(card.getId());
        }
    }

    private void validateSufficientFunds(Card sourceCard, BigDecimal amount) {
        if (sourceCard.getBalance().compareTo(amount) < 0) {
            throw BusinessException.insufficientFunds(amount, sourceCard.getBalance());
        }
    }

    private void validateCardOwnership(Card sourceCard, Long userId) {
        if (!sourceCard.getOwner().getId().equals(userId)) {
            throw ResourceNotFoundException.card(sourceCard.getId());
        }
    }

    // --- Lookup --- //

    private Card findCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> ResourceNotFoundException.card(cardId));
    }

    // --- Transfer execution --- //

    private void executeBalanceTransfer(Card source, Card destination, BigDecimal amount) {
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
    }

    private Transfer createTransfer(TransferRequest request, Card source, Card destination) {
        Transfer transfer = Transfer.builder()
                .sourceCard(source)
                .destinationCard(destination)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(TransferStatus.SUCCESS)
                .build();

        return transferRepository.save(transfer);
    }

    // --- Events --- //

    private void publishTransferEvent(Transfer transfer, Card sourceCard, Card destinationCard) {
        TransferEvent event = new TransferEvent(
                transfer.getId(),
                sourceCard.getOwner().getId(),
                destinationCard.getOwner().getId(),
                CardMaskingUtil.maskCardNumber(encryptionUtil.decrypt(sourceCard.getCardNumber())),
                CardMaskingUtil.maskCardNumber(encryptionUtil.decrypt(destinationCard.getCardNumber())),
                transfer.getAmount(),
                transfer.getTimestamp(),
                transfer.getStatus().name()
        );

        kafkaProducerService.sendTransferEvent(event);
    }
}
