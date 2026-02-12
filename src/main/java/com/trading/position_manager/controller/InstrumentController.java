package com.trading.position_manager.controller;

import com.trading.position_manager.dto.InstrumentRequestDTO;
import com.trading.position_manager.model.Instrument;
import com.trading.position_manager.service.InstrumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService service;

    @GetMapping
    public List<Instrument> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Instrument findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Instrument create(@Valid @RequestBody InstrumentRequestDTO dto) {
        return service.create(dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}