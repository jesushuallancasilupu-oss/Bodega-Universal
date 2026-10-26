package com.pe.universal.service;

import com.pe.universal.datos.LineaProducto;
import com.pe.universal.datos.Resumenes.*;
import com.pe.universal.manager.VentaManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VentaService {
    private final VentaManager gestor;
    public VentaService(VentaManager gestor) { this.gestor = gestor; }

    public VentaVista registrarVentaPorPedido(Integer vendedorId, Integer pedidoId) {
        return gestor.registrarPorPedido(vendedorId, pedidoId);
    }
    public VentaVista registrarVentaPresencial(Integer vendedorId, List<LineaProducto> productos) {
        return gestor.registrarPresencial(vendedorId, productos);
    }
    public PagoVista registrarPago(Integer vendedorId, Integer ventaId, String metodoPago, BigDecimal monto) {
        return gestor.registrarPago(vendedorId, ventaId, metodoPago, monto);
    }
    public ComprobanteVista emitirComprobante(Integer vendedorId, Integer ventaId, String tipo) {
        return gestor.emitirComprobante(vendedorId, ventaId, tipo);
    }
    public VentaVista consultarVenta(Integer usuarioId, Integer ventaId) {
        return gestor.consultar(usuarioId, ventaId);
    }
    public List<VentaVista> historialCliente(Integer clienteId) { return gestor.historialCliente(clienteId); }
    public List<VentaVista> ventasPorFecha(Integer usuarioId, LocalDateTime desde, LocalDateTime hasta) {
        return gestor.consultarPorFecha(usuarioId, desde, hasta);
    }

    public List<VentaVista> ventasPorFecha(Integer usuarioId, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null)
            throw new com.pe.universal.exception.NegocioException("Indique ambas fechas");
        return gestor.consultarPorFecha(usuarioId, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }
    public ReporteVentasVista reporteVentas(Integer supervisorId, LocalDate desde, LocalDate hasta) {
        return gestor.reporteVentas(supervisorId, desde, hasta);
    }

    public PagoVista consultarPago(Integer usuarioId, Integer ventaId) {
        return gestor.consultarPago(usuarioId, ventaId);
    }
    public ComprobanteVista consultarComprobante(Integer usuarioId, Integer ventaId) {
        return gestor.consultarComprobante(usuarioId, ventaId);
    }
}
