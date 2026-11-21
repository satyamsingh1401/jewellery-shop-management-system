package com.myhourly.jewellery.inventory.repository;

import com.myhourly.jewellery.inventory.entity.CancelledOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancelledOrderRepository extends JpaRepository<CancelledOrder, String> {
}
