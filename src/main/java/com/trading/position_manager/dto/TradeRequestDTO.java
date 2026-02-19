package com.trading.position_manager.dto;

import com.trading.position_manager.model.TradeDirection;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder

public class TradeRequestDTO {
    @NotNull (message = "Instrumento é obrigatório")
    private Long instrumentId;

    @NotNull(message = "Data do trade é obrigatória")
    private LocalDate tradeDate;

    @NotNull(message = "Direção é obrigatória")
    private TradeDirection direction;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser positiva")
    private BigDecimal quantity;

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    private BigDecimal price;

    @NotBlank(message = "Contraparte é obrigatória")
    private String counterparty;
}
