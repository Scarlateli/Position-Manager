package com.trading.position_manager.dto;

import com.trading.position_manager.model.InstrumentType;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
public class InstrumentRequestDTO {

    @NotBlank(message = "Ticker é obrigatório")
    @Size(max = 10, message = "Ticker deve ter no máximo 10 caracteres")
    private String ticker;

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotNull(message = "Tipo é obrigatório")
    private InstrumentType type;

    @NotBlank(message = "Moeda é obrigatória")
    @Size(min = 3, max = 3, message = "Moeda deve ter 3 caracteres (ex: BRL, USD)")
    private String currency;
}