package com.trading.position_manager.controller;

import org.springframework.web.bind.annotation.*;
import com.trading.position_manager.service.PositionService;
import com.trading.position_manager.dto.PositionResponseDTO;
import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping("/{instrumentId}")
    public PositionResponseDTO getPositionByInstrument(@PathVariable Long instrumentId) {
        return positionService.findByInstrument(instrumentId);
    }

    @GetMapping
    public List<PositionResponseDTO> getAllActivePositions() {
        return positionService.findAllActivePositions();
    }
}