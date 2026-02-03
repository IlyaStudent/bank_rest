package com.example.bankcards.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferEvent(
        Long transferId,
        Long senderUserId,
        Long recipientUserId,
        String senderCardMasked,
        String recipientCardMasked,
        BigDecimal amount,
        Instant timestamp,
        String status
) {
}
