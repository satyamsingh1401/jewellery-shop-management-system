package com.myhourly.jewellery.inventory.repository;

import com.myhourly.jewellery.inventory.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
