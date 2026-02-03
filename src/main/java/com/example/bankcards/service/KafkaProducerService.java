package com.example.bankcards.service;

import com.example.bankcards.event.TransferEvent;

public interface KafkaProducerService {
    void sendTransferEvent(TransferEvent event);
}
