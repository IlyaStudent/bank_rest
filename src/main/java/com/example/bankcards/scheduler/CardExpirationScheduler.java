package com.example.bankcards.scheduler;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private final CardRepository cardRepository;
    private final TaskExecutor batchExecutor;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    void markExpiredCards() {
        log.info("Starting expired cards check");

        List<Card> expiredCards = cardRepository.findActiveCardsWithExpirationDate(LocalDate.now());

        List<List<Card>> batches = partition(expiredCards);

        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(() -> processBatch(batch), batchExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Expired cards check completed: {} cards updated", expiredCards.size());
    }

    private void processBatch(List<Card> batch) {
        batch.forEach(card -> card.setStatus(CardStatus.EXPIRED));
        cardRepository.saveAll(batch);
    }

    private <T> List<List<T>> partition(List<T> list) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 100) {
            partitions.add(list.subList(i, Math.min(i + 100, list.size())));
        }
        return partitions;
    }
}
