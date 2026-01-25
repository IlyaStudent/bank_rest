package com.example.bankcards.dto.transfer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {
    @NotNull
    private Long sourceCardId;

    @NotNull
    private Long destinationCardId;

    @NotNull
    @Positive
    private BigDecimal amount;
    private String description;
}
