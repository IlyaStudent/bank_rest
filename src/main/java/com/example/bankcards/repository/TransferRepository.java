package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @EntityGraph(attributePaths = {"sourceCard", "destinationCard"})
    Page<Transfer> findBySourceCardOwnerIdOrDestinationCardOwnerId(
            Long sourceOwnerId,
            Long destinationOwnerId,
            Pageable pageable
    );

    Page<Transfer> findBySourceCardOwnerId(
            Long sourceOwnerId,
            Pageable pageable
    );

    Page<Transfer> findByDestinationCardOwnerId(
            Long destinationOwnerId,
            Pageable pageable
    );

}
