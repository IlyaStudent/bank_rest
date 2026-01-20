package com.example.bankcards.dto.card;

import com.example.bankcards.validation.CardNumber;
import com.example.bankcards.validation.Cvv;
import com.example.bankcards.validation.ExpiryDate;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCardRequest {
    @NotBlank
    @CardNumber
    private String cardNumber;

    @NotBlank
    private String holderName;

    @NotBlank
    @ExpiryDate
    private String expiryDate;

    @NotBlank
    @Cvv
    private String cvv;
}
