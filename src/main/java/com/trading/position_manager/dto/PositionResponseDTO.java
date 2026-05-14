package com.trading.position_manager.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.trading.position_manager.model.Position;

public record PositionResponseDTO(
        Long id,
        Long instrumentId,
        String instrumentTicker,
        LocalDate positionDate,
        BigDecimal quantity,
        BigDecimal averagePrice,
        LocalDateTime updatedAt
) {

    public static PositionResponseDTO from(Position position) {
        return new PositionResponseDTO(
                position.getId(),
                position.getInstrument().getId(),
                position.getInstrument().getTicker(),
                position.getPositionDate(),
                position.getQuantity(),
                position.getAveragePrice(),
                position.getUpdatedAt()
        );
    }
}
