package com.trading.position_manager.service;

import com.trading.position_manager.dto.PositionResponseDTO;
import com.trading.position_manager.exception.ResourceNotFoundException;
import com.trading.position_manager.model.TradeDirection;
import com.trading.position_manager.model.Instrument;
import com.trading.position_manager.model.Position;
import com.trading.position_manager.model.Trade;
import com.trading.position_manager.model.TradeStatus;
import com.trading.position_manager.repository.InstrumentRepository;
import com.trading.position_manager.repository.PositionRepository;
import com.trading.position_manager.repository.TradeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;

    @Transactional
    public Position recalculate(Long instrumentId) {

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found with id: " + instrumentId
                ));

        List<Trade> settledTrades =tradeRepository.findByInstrumentIdAndStatus(instrumentId,TradeStatus.SETTLED);

        if (settledTrades.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No settled trades found for instrument id: " + instrumentId
            );
        }

        BigDecimal netQuantity = BigDecimal.ZERO;
        BigDecimal totalBuyQuantity = BigDecimal.ZERO;
        BigDecimal totalBuyValue = BigDecimal.ZERO;

        for (Trade trade : settledTrades) {

            BigDecimal quantity = trade.getQuantity();
            BigDecimal price = trade.getPrice();

            if (trade.getDirection() == TradeDirection.BUY) {

                netQuantity = netQuantity.add(quantity);

                totalBuyQuantity = totalBuyQuantity.add(quantity);

                totalBuyValue = totalBuyValue.add(quantity.multiply(price));

            } else if (trade.getDirection() == TradeDirection.SELL) {

                netQuantity = netQuantity.subtract(quantity);
            }
        }

        BigDecimal averagePrice = BigDecimal.ZERO;

        if (totalBuyQuantity.compareTo(BigDecimal.ZERO) > 0) {

            averagePrice = totalBuyValue.divide(totalBuyQuantity,6,RoundingMode.HALF_UP);
        }

        Position position = positionRepository.findByInstrumentId(instrumentId)
                .orElseGet(() -> Position.builder()
                        .instrument(instrument)
                        .positionDate(LocalDate.now())
                        .quantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                        .averagePrice(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                        .build()
                );

        position.setPositionDate(LocalDate.now());

        position.setQuantity(netQuantity.setScale(4, RoundingMode.HALF_UP));

        position.setAveragePrice(averagePrice.setScale(6, RoundingMode.HALF_UP));

        return positionRepository.save(position);
    }

    @Transactional(readOnly = true)
    public PositionResponseDTO findByInstrument(Long instrumentId) {

        Position position = positionRepository.findByInstrumentId(instrumentId)
                .orElseGet(() -> {

                    Instrument instrument = instrumentRepository.findById(instrumentId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Instrument not found with id: " + instrumentId
                            ));

                    return Position.builder()
                            .instrument(instrument)
                            .positionDate(LocalDate.now())
                            .quantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .averagePrice(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                            .build();
                });

        return PositionResponseDTO.from(position);
    }

    @Transactional(readOnly = true)
    public List<PositionResponseDTO> findAllActivePositions() {

        return positionRepository.findAllByInstrumentActiveTrue()
                .stream()
                .map(PositionResponseDTO::from)
                .toList();
    }
}