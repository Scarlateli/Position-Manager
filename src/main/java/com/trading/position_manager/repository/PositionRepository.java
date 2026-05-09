package com.trading.position_manager.repository;

import com.trading.position_manager.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByInstrumentId(Long instrumentId);

    List<Position> findAllByInstrumentActiveTrue();
}