package com.trading.position_manager.controller;

import com.trading.position_manager.dto.TradeRequestDTO;
import com.trading.position_manager.model.Trade;
import com.trading.position_manager.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {
    private final TradeService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Trade create(@Valid @RequestBody TradeRequestDTO dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}/settle")
    public Trade settle(@PathVariable Long id) {
        return service.settle(id);
    }

    @PatchMapping("/{id}/cancel")
    public Trade cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}