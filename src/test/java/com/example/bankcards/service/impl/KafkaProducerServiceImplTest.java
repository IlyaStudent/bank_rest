package com.example.bankcards.service.impl;

import com.example.bankcards.event.TransferEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceImplTest {

    private static final String TOPIC = "bank.transfers";

    @Mock
    private KafkaTemplate<String, TransferEvent> kafkaTemplate;

    @InjectMocks
    private KafkaProducerServiceImpl kafkaProducerService;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<TransferEvent> eventCaptor;

    private TransferEvent transferEvent;

    @BeforeEach
    void setUp() {
        transferEvent = new TransferEvent(
                1L,
                100L,
                200L,
                "**** **** **** 1234",
                "**** **** **** 5678",
                new BigDecimal("500.00"),
                Instant.now(),
                "SUCCESS"
        );
    }

    @Test
    @DisplayName("Should send transfer event to Kafka with correct parameters")
    void shouldSendTransferEventToKafkaWithCorrectParameters() {
        CompletableFuture<SendResult<String, TransferEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(String.class), any(String.class), any(TransferEvent.class)))
                .thenReturn(future);

        kafkaProducerService.sendTransferEvent(transferEvent);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo(transferEvent.transferId().toString());
        assertThat(eventCaptor.getValue()).isEqualTo(transferEvent);
    }

    @Test
    @DisplayName("Should use transfer ID as message key")
    void shouldUseTransferIdAsMessageKey() {
        CompletableFuture<SendResult<String, TransferEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(String.class), any(String.class), any(TransferEvent.class)))
                .thenReturn(future);

        kafkaProducerService.sendTransferEvent(transferEvent);

        verify(kafkaTemplate).send(TOPIC, "1", transferEvent);
    }

    @Test
    @DisplayName("Should handle successful send completion")
    void shouldHandleSuccessfulSendCompletion() {
        CompletableFuture<SendResult<String, TransferEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(String.class), any(String.class), any(TransferEvent.class)))
                .thenReturn(future);

        kafkaProducerService.sendTransferEvent(transferEvent);

        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0),
                0L,
                0,
                System.currentTimeMillis(),
                0,
                0
        );
        ProducerRecord<String, TransferEvent> producerRecord = new ProducerRecord<>(TOPIC, transferEvent);
        SendResult<String, TransferEvent> sendResult = new SendResult<>(producerRecord, recordMetadata);

        future.complete(sendResult);

        verify(kafkaTemplate).send(TOPIC, "1", transferEvent);
    }

    @Test
    @DisplayName("Should handle send failure gracefully")
    void shouldHandleSendFailureGracefully() {
        CompletableFuture<SendResult<String, TransferEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(String.class), any(String.class), any(TransferEvent.class)))
                .thenReturn(future);

        kafkaProducerService.sendTransferEvent(transferEvent);

        future.completeExceptionally(new RuntimeException("Kafka unavailable"));

        verify(kafkaTemplate).send(TOPIC, "1", transferEvent);
    }

    @Test
    @DisplayName("Should send event with all fields populated")
    void shouldSendEventWithAllFieldsPopulated() {
        CompletableFuture<SendResult<String, TransferEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(String.class), any(String.class), any(TransferEvent.class)))
                .thenReturn(future);

        kafkaProducerService.sendTransferEvent(transferEvent);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        TransferEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.transferId()).isEqualTo(1L);
        assertThat(capturedEvent.senderUserId()).isEqualTo(100L);
        assertThat(capturedEvent.recipientUserId()).isEqualTo(200L);
        assertThat(capturedEvent.senderCardMasked()).isEqualTo("**** **** **** 1234");
        assertThat(capturedEvent.recipientCardMasked()).isEqualTo("**** **** **** 5678");
        assertThat(capturedEvent.amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(capturedEvent.status()).isEqualTo("SUCCESS");
    }
}
