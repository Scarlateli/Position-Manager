package com.trading.position_manager.repository;

import com.trading.position_manager.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    // Busca instrumento pelo ticker (ex: PETR4)
    Optional<Instrument> findByTicker(String ticker);

    // Lista apenas instrumentos ativos
    List<Instrument> findByActiveTrue();

    // Verifica se ticker já existe
    boolean existsByTicker(String ticker);
}