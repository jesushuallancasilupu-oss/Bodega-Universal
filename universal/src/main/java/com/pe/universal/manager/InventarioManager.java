package com.pe.universal.manager;

import com.pe.universal.dao.MovimientoInventarioRepository;
import com.pe.universal.dao.ProductoRepository;
import com.pe.universal.datos.Resumenes;
import com.pe.universal.datos.Resumenes.*;
import com.pe.universal.exception.NoEncontradoException;
import com.pe.universal.model.MovimientoInventario;
import com.pe.universal.model.Producto;
import com.pe.universal.model.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventarioManager {
    private final ProductoRepository productos;
    private final MovimientoInventarioRepository movimientos;
    private final UsuarioManager gestorUsuarios;

    public InventarioManager(ProductoRepository productos, MovimientoInventarioRepository movimientos,
            UsuarioManager gestorUsuarios) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.gestorUsuarios = gestorUsuarios;
    }

    @Transactional
    public MovimientoVista registrarMovimiento(Integer supervisorId, Integer productoId, String tipo,
            Integer cantidad, String motivo) {
        Usuario supervisor = gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        Reglas.exigir(tipo != null && cantidad != null, "Indique tipo y cantidad");
        String tipoMovimiento = tipo.toUpperCase();
        Reglas.exigir(List.of("ENTRADA", "SALIDA", "AJUSTE").contains(tipoMovimiento), "Tipo de movimiento inválido");
        Reglas.exigir(tipoMovimiento.equals("AJUSTE") ? cantidad != 0 : cantidad > 0, "Cantidad inválida");
        Producto producto = productos.bloquear(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
        long cambio = tipoMovimiento.equals("SALIDA") ? -(long) cantidad : cantidad;
        long nuevo = producto.getStockActual() + cambio;
        Reglas.exigir(nuevo >= 0 && nuevo <= Integer.MAX_VALUE, "Stock insuficiente o demasiado alto");
        producto.setStockActual((int) nuevo);
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setSupervisor(supervisor);
        movimiento.setTipo(tipoMovimiento);
        movimiento.setCantidad(cantidad);
        movimiento.setFechaHora(LocalDateTime.now());
        movimiento.setMotivo(motivo);
        return Resumenes.movimiento(movimientos.save(movimiento));
    }

    @Transactional(readOnly = true)
    public List<MovimientoVista> historial(Integer supervisorId) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        return movimientos.findAllByOrderByFechaHoraDesc().stream().map(Resumenes::movimiento).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoVista> alertasStock(Integer supervisorId) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        return productos.bajos((byte) 1).stream().map(Resumenes::producto).toList();
    }

    @Transactional(readOnly = true)
    public ReporteInventarioVista reporte(Integer supervisorId) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");

        List<ProductoVista> inventarioActual = productos.findAll().stream()
                .map(Resumenes::producto).toList();

        long activos = inventarioActual.stream()
                .filter(producto -> producto.estado() == 1)
                .count();

        long bajos = inventarioActual.stream()
                .filter(producto -> producto.estado() == 1 && producto.stock() <= producto.stockMinimo())
                .count();

        return new ReporteInventarioVista(activos, bajos, inventarioActual);
    }
}
