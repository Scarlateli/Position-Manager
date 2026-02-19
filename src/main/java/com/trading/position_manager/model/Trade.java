package com.trading.position_manager.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    private TradeDirection direction;

    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 18, scale = 6)
    private BigDecimal price;

    private String counterparty;

    @Enumerated(EnumType.STRING)
    private TradeStatus status;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = TradeStatus.PENDING;
        if (settlementDate == null && tradeDate != null)
            settlementDate = tradeDate.plusDays(2);
    }

    public BigDecimal getTotalValue() {
        return quantity.multiply(price);
    }
}