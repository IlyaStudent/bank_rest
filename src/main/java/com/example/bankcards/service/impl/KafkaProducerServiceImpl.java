package com.example.bankcards.service.impl;

import com.example.bankcards.event.TransferEvent;
import com.example.bankcards.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private static final String TOPIC = "bank.transfers";

    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;

    @Override
    public void sendTransferEvent(TransferEvent event) {
        log.debug("Sending transfer event to Kafka: transferId={}", event.transferId());

        kafkaTemplate.send(TOPIC, event.transferId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info(
                                "Transfer event sent to Kafka: transferId={}, partition={}, offset={}",
                                event.transferId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    } else {
                        log.error(
                                "Failed to send transfer event to Kafka: transferId={}",
                                event.transferId(),
                                ex
                        );
                    }
                });

    }
}
