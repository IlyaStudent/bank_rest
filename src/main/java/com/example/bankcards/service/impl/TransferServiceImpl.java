package com.example.bankcards.service.impl;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final TransferMapper transferMapper;

    @Override
    public TransferResponse transferMoney(TransferRequest transferRequest) {
        Long sourceCardId = transferRequest.getSourceCardId();
        Long destinationCardId = transferRequest.getDestinationCardId();
        BigDecimal amount = transferRequest.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalidTransferAmount();
        }

        if (sourceCardId.equals(destinationCardId)) {
            throw BusinessException.sameCardTransfer();
        }

        Card sourceCard = cardRepository.findById(sourceCardId)
                .orElseThrow(() -> ResourceNotFoundException.card(sourceCardId));
        Card destinationCard = cardRepository.findById(destinationCardId)
                .orElseThrow(() -> ResourceNotFoundException.card(destinationCardId));

        validateCardStatus(sourceCard);
        validateCardStatus(destinationCard);

        BigDecimal sourceCardBalance = sourceCard.getBalance();
        BigDecimal destinationCardBalance = destinationCard.getBalance();

        if (sourceCardBalance.compareTo(amount) < 0) {
            throw BusinessException.insufficientFunds(amount, sourceCardBalance);
        }

        sourceCard.setBalance(sourceCard.getBalance().subtract(amount));
        destinationCard.setBalance(destinationCardBalance.add(amount));

        Transfer transfer = Transfer.builder()
                .sourceCard(sourceCard)
                .destinationCard(destinationCard)
                .amount(amount)
                .description(transferRequest.getDescription())
                .status(TransferStatus.SUCCESS)
                .build();

        transfer = transferRepository.save(transfer);

        return transferMapper.toResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponse> getTransferHistory(Long userId, Pageable pageable) {
        return transferRepository
                .findBySourceCardOwnerIdOrDestinationCardOwnerId(userId, userId, pageable)
                .map(transferMapper::toResponse);
    }

    private void validateCardStatus(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw BusinessException.cardBlocked(card.getId());
        }

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw BusinessException.cardExpired(card.getId());
        }
    }
}
