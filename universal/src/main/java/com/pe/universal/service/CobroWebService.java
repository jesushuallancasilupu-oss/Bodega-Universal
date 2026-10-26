package com.pe.universal.service;

import com.pe.universal.datos.LineaProducto;
import com.pe.universal.datos.Resumenes.VentaVista;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CobroWebService {
    private final VentaService ventas;
    public CobroWebService(VentaService ventas) { this.ventas = ventas; }

    @Transactional
    public VentaVista presencial(Integer vendedor, List<LineaProducto> lineas,
            String metodo, BigDecimal recibido) {
        VentaVista venta = ventas.registrarVentaPresencial(vendedor, lineas);
        finalizar(vendedor, venta, metodo, recibido);
        return venta;
    }

    @Transactional
    public VentaVista desdePedido(Integer vendedor, Integer pedido, String metodo, BigDecimal recibido) {
        VentaVista venta = ventas.registrarVentaPorPedido(vendedor, pedido);
        finalizar(vendedor, venta, metodo, recibido);
        return venta;
    }

    private void finalizar(Integer vendedor, VentaVista venta, String metodo, BigDecimal recibido) {
        ventas.registrarPago(vendedor, venta.id(), metodo, recibido);
        ventas.emitirComprobante(vendedor, venta.id(), "TICKET");
    }
}
