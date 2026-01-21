package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransferService {
    TransferResponse transferMoney(@NotNull TransferRequest transferRequest);

    Page<TransferResponse> getTransferHistory(@NotNull Long userId, Pageable pageable);
}
