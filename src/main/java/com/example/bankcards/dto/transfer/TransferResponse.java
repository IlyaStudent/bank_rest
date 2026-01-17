package com.example.bankcards.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {
    private Long id;
    private String sourceCardMasked;
    private String destinationCardMasked;
    private BigDecimal amount;
    private Instant timestamp;
    private String status;
}
