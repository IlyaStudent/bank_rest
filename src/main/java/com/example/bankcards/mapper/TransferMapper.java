package com.example.bankcards.mapper;

import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransferMapper {

    private final EncryptionUtil encryptionUtil;

    public TransferResponse toResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .sourceCardMasked(maskCard(transfer.getSourceCard()))
                .destinationCardMasked(maskCard(transfer.getDestinationCard()))
                .amount(transfer.getAmount())
                .timestamp(transfer.getTimestamp())
                .status(transfer.getStatus().name())
                .build();
    }

    private String maskCard(Card card) {
        String decrypted = encryptionUtil.decrypt(card.getCardNumber());
        return CardMaskingUtil.maskCardNumber(decrypted);
    }

}
