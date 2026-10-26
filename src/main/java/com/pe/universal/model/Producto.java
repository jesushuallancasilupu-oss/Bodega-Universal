
package com.pe.universal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(name = "cantidad_por_venta", nullable = false)
    private Integer cantidadPorVenta = 1;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "estado", nullable = false)
    private Byte estado;
}
