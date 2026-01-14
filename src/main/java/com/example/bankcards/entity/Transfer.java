package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "transfers",
        indexes = {
                @Index(name = "idx_transfer_source_card_id", columnList = "source_card_id"),
                @Index(name = "idx_transfer_destination_card_id", columnList = "destination_card_id"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sourceCard", "destinationCard"})
@EqualsAndHashCode(of = "id")
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_card_id", nullable = false)
    private Card sourceCard;

    @ManyToOne
    @JoinColumn(name = "destination_card_id", nullable = false)
    private Card destinationCard;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant timeStamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @PrePersist
    protected void onCreate() {
        timeStamp = Instant.now();
        if (status == null) {
            status = TransferStatus.PENDING;
        }
    }
}
