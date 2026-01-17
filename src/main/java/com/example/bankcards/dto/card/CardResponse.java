package com.example.bankcards.dto.card;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardResponse {
    private Long id;
    private String maskedCardNumber;
    private String holderName;
    private String expiryDate;
    private String status;
    private BigDecimal balance;
    private Instant createdAt;
}
