package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfers", description = "Money transfer operations")
@RestController
@RequiredArgsConstructor
@RequestMapping("${end.point.transfers}")
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Transfer money", description = "Transfers money between two cards")
    @ApiResponse(responseCode = "201", description = "Transfer completed successfully")
    @ApiResponse(responseCode = "404", description = "Source or destination card not found")
    @ApiResponse(responseCode = "422", description = "Business error (insufficient funds, blocked card, same card transfer, etc.)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transferMoney(
            @RequestBody @Valid TransferRequest transferRequest
    ) {
        return transferService.transferMoney(transferRequest);
    }

    @Operation(summary = "Get transfer history", description = "Returns paginated transfer history for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Transfer history retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping
    public Page<TransferResponse> getTransferHistory(
            Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        return transferService.getTransferHistory(userId, pageable);
    }
}
