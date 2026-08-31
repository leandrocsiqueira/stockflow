package com.leandro.stockflow.repository;

import com.leandro.stockflow.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementRepository
    extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {}
