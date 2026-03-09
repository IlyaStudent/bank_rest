package com.example.bankcards.service.impl;

import com.example.bankcards.event.TransferEvent;
import com.example.bankcards.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private static final String TOPIC = "bank.transfers";

    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;

    @Async("kafkaExecutor")
    @Override
    public CompletableFuture<Void> sendTransferEventAsync(TransferEvent event) {
        return kafkaTemplate.send(TOPIC, event.transferId().toString(), event)
                .thenAccept(result -> log.info(
                        "Transfer event sent: transferId={}, partition={}, offset={}",
                        event.transferId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                ))
                .exceptionally(ex -> {
                    log.error("Failed to send transfer event: transferId={}", event.transferId(), ex);
                    return null;
                })
                .toCompletableFuture();
    }
}
