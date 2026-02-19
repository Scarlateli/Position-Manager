package com.trading.position_manager.repository;

import com.trading.position_manager.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TradeRepository
        extends JpaRepository<Trade, Long> {
    List<Trade> findByInstrumentId(Long instrumentId);

    @Query("SELECT t FROM Trade t WHERE t.instrument.id = :id "
            + "AND t.status = 'SETTLED'")
    List<Trade> findSettledByInstrument(Long id);
}