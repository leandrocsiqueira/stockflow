package com.leandro.stockflow.service;

import com.leandro.stockflow.dto.ReplenishmentOrderResponse;
import com.leandro.stockflow.entity.ReplenishmentOrder;
import com.leandro.stockflow.entity.ReplenishmentStatus;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.mapper.ReplenishmentOrderMapper;
import com.leandro.stockflow.repository.ReplenishmentOrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplenishmentOrderService {

  private final ReplenishmentOrderRepository replenishmentOrderRepository;

  public ReplenishmentOrderService(ReplenishmentOrderRepository replenishmentOrderRepository) {
    this.replenishmentOrderRepository = replenishmentOrderRepository;
  }

  @Transactional(readOnly = true)
  public List<ReplenishmentOrderResponse> findPending() {
    return replenishmentOrderRepository.findByStatus(ReplenishmentStatus.PENDING).stream()
        .map(ReplenishmentOrderMapper::toResponse)
        .toList();
  }

  @Transactional
  public ReplenishmentOrderResponse complete(Long id) {
    ReplenishmentOrder order =
        replenishmentOrderRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Replenishment order not found with id: " + id));
    order.complete();
    return ReplenishmentOrderMapper.toResponse(order);
  }
}
