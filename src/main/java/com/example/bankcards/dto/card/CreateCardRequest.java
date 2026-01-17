package com.example.bankcards.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCardRequest {
    @NotBlank
    private String cardNumber;

    @NotBlank
    private String holderName;

    @NotBlank
    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$")
    private String expiryDate;

    @NotBlank
    @Pattern(regexp = "^\\d{3}$")
    private String cvv;
}
