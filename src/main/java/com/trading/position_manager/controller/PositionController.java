package com.trading.position_manager.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.trading.position_manager.service.PositionService;

import lombok.RequiredArgsConstructor;

import com.trading.position_manager.dto.PositionResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @GetMapping("/{instrumentId}")
    @ResponseStatus(HttpStatus.OK)
    public PositionResponseDTO getPositionByInstrument(@PathVariable Long instrumentId) {
        return positionService.findByInstrument(instrumentId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<PositionResponseDTO> getAllActivePositions() {
        return positionService.findAllActivePositions();
    }
}