package com.example.bankcards.mapper;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class CardMapper {
    private final EncryptionUtil encryptionUtil;

    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

    public CardResponse toResponse(Card card) {
        String decryptedCardNumber = encryptionUtil.decrypt(card.getCardNumber());
        String maskedCardNumber = CardMaskingUtil.maskCardNumber(decryptedCardNumber);

        return CardResponse.builder()
                .id(card.getId())
                .maskedCardNumber(maskedCardNumber)
                .holderName(card.getHolderName())
                .expiryDate(card.getExpiryDate().format(EXPIRY_FORMATTER))
                .status(card.getStatus().name())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
