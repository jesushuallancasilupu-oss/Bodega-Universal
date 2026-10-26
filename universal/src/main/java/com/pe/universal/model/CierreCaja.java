package com.pe.universal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cierre_caja")
@Getter
@Setter
@NoArgsConstructor
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cierre")
    private Integer idCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "monto_esperado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoEsperado;

    @Column(name = "monto_registrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoRegistrado;

    // Valor calculado por MySQL.
    @Column(name = "diferencia", insertable = false, updatable = false, precision = 11, scale = 2)
    private BigDecimal diferencia;
}
