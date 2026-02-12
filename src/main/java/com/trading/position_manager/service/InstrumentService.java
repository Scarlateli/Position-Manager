package com.trading.position_manager.service;

import com.trading.position_manager.dto.InstrumentRequestDTO;
import com.trading.position_manager.exception.BusinessException;
import com.trading.position_manager.exception.ResourceNotFoundException;
import com.trading.position_manager.model.Instrument;
import com.trading.position_manager.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository repository;

    public List<Instrument> findAll() {
        return repository.findByActiveTrue();
    }

    public Instrument findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrumento não encontrado com ID: " + id));
    }

    public Instrument create(InstrumentRequestDTO dto) {
        if (repository.existsByTicker(dto.getTicker())) {
            throw new BusinessException("Ticker já cadastrado: " + dto.getTicker());
        }

        Instrument instrument = Instrument.builder()
                .ticker(dto.getTicker().toUpperCase())
                .name(dto.getName())
                .type(dto.getType())
                .currency(dto.getCurrency().toUpperCase())
                .active(true)
                .build();

        return repository.save(instrument);
    }

    public void delete(Long id) {
        Instrument instrument = findById(id);
        instrument.setActive(false);
        repository.save(instrument);
    }
}
