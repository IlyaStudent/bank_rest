package com.example.bankcards.service;

import com.example.bankcards.event.TransferEvent;

import java.util.concurrent.CompletableFuture;

public interface KafkaProducerService {
    CompletableFuture<Void> sendTransferEventAsync(TransferEvent event);
}
