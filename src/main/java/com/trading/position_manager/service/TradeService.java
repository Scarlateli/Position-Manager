package com.trading.position_manager.service;

import com.trading.position_manager.dto.TradeRequestDTO;
import com.trading.position_manager.exception.BusinessException;
import com.trading.position_manager.exception.ResourceNotFoundException;
import com.trading.position_manager.model.*;
import com.trading.position_manager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepo;
    private final InstrumentRepository instrumentRepo;

    @Transactional
    public Trade create(TradeRequestDTO dto) {
        Instrument inst = instrumentRepo.findById(dto.getInstrumentId())
                .orElseThrow(() -> new BusinessException("Instrumento não encontrado"));

        if (!inst.getActive())
            throw new BusinessException("Instrumento inativo");

        Trade trade = Trade.builder()
                .instrument(inst)
                .tradeDate(dto.getTradeDate())
                .direction(dto.getDirection())
                .quantity(dto.getQuantity())
                .price(dto.getPrice())
                .counterparty(dto.getCounterparty())
                .status(TradeStatus.PENDING)
                .build();
        return tradeRepo.save(trade);
    }

    @Transactional
    public Trade settle(Long id) {
        Trade t = tradeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trade não encontrado com ID: " + id));
        if (t.getStatus() == TradeStatus.CANCELLED)
            throw new BusinessException("Trade cancelado");
        t.setStatus(TradeStatus.SETTLED);
        return tradeRepo.save(t);
    }

    @Transactional
    public Trade cancel(Long id) {
        Trade t = tradeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trade não encontrado com ID: " + id));
        if (t.getStatus() == TradeStatus.SETTLED)
            throw new BusinessException("Trade liquidado");
        t.setStatus(TradeStatus.CANCELLED);
        return tradeRepo.save(t);
    }
}