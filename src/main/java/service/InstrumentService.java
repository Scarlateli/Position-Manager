package com.trading.position_manager.service;

import com.trading.position_manager.dto.InstrumentRequestDTO;
import com.trading.position_manager.model.Instrument;
import com.trading.position_manager.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository repository;

    // Lista todos os instrumentos ativos
    public List<Instrument> findAll() {
        return repository.findByActiveTrue();
    }

    // Busca instrumento por ID
    public Instrument findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Instrumento não encontrado com ID: " + id));
    }

    // Cria novo instrumento
    public Instrument create(InstrumentRequestDTO dto) {
        // Valida se ticker já existe
        if (repository.existsByTicker(dto.getTicker())) {
            throw new RuntimeException("Ticker já cadastrado: " + dto.getTicker());
        }

        // Constrói o objeto Instrument usando o Builder do Lombok
        Instrument instrument = Instrument.builder()
                .ticker(dto.getTicker().toUpperCase())
                .name(dto.getName())
                .type(dto.getType())
                .currency(dto.getCurrency().toUpperCase())
                .active(true)
                .build();

        return repository.save(instrument);
    }

    // "Deleta" (soft delete - apenas desativa)
    public void delete(Long id) {
        Instrument instrument = findById(id);
        instrument.setActive(false);
        repository.save(instrument);
    }
}