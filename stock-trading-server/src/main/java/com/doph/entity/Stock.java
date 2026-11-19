package com.doph.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Getter
@Setter
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_name", unique = true, nullable = false)
    private String stockSymbol;

    private double price;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    private String currency;
}
