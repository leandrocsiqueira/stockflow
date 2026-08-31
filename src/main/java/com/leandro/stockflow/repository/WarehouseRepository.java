package com.leandro.stockflow.repository;

import com.leandro.stockflow.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {}
