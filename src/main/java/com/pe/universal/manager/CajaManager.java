package com.pe.universal.manager;

import com.pe.universal.dao.*;
import com.pe.universal.datos.Resumenes;
import com.pe.universal.datos.Resumenes.CierreVista;
import com.pe.universal.exception.NegocioException;
import com.pe.universal.exception.NoEncontradoException;
import com.pe.universal.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CajaManager {
    private final VentaRepository ventas;
    private final PagoRepository pagos;
    private final CierreCajaRepository cierres;
    private final UsuarioRepository usuarios;
    private final UsuarioManager gestorUsuarios;

    public CajaManager(VentaRepository ventas, PagoRepository pagos,
            CierreCajaRepository cierres, UsuarioRepository usuarios, UsuarioManager gestorUsuarios) {
        this.ventas = ventas; this.pagos = pagos; this.cierres = cierres;
        this.usuarios = usuarios; this.gestorUsuarios = gestorUsuarios;
    }

    @Transactional(readOnly = true)
    public BigDecimal efectivoPendiente(Integer vendedorId) {
        gestorUsuarios.exigir(vendedorId, "VENDEDOR");
        BigDecimal total = BigDecimal.ZERO;
        for (Venta venta : ventas.findByVendedorIdUsuarioAndCierreIsNull(vendedorId)) {
            Pago pago = pagos.findByVentaIdVenta(venta.getIdVenta()).orElseThrow(
                    () -> new NegocioException("Hay una venta sin pago: " + venta.getIdVenta()));
            if ("EFECTIVO".equals(pago.getMetodoPago()))
                total = total.add(pago.getMontoPagado().subtract(pago.getVuelto()));
        }
        return total;
    }

    @Transactional
    public CierreVista cerrarCaja(Integer vendedorId, BigDecimal montoRegistrado) {
        gestorUsuarios.exigir(vendedorId, "VENDEDOR");

        Usuario vendedor = usuarios.bloquear(vendedorId)
                .orElseThrow(() -> new NoEncontradoException("Vendedor no encontrado"));
        List<Venta> pendientes = ventas.findByVendedorIdUsuarioAndCierreIsNull(vendedorId);
        Reglas.exigir(!pendientes.isEmpty(), "No hay ventas pendientes por cerrar");
        BigDecimal esperado = BigDecimal.ZERO;
        for (Venta venta : pendientes) {
            Pago pago = pagos.findByVentaIdVenta(venta.getIdVenta()).orElseThrow(
                    () -> new NegocioException("Registre el pago de la venta " + venta.getIdVenta()));
            if (pago.getMetodoPago().equals("EFECTIVO"))
                esperado = esperado.add(pago.getMontoPagado().subtract(pago.getVuelto()));
        }
        CierreCaja cierre = new CierreCaja(); cierre.setVendedor(vendedor);
        cierre.setFechaHora(LocalDateTime.now());
        cierre.setMontoEsperado(Reglas.noNegativo(esperado));
        cierre.setMontoRegistrado(Reglas.noNegativo(montoRegistrado));
        cierres.save(cierre);
        for (Venta venta : pendientes) venta.setCierre(cierre);
        return Resumenes.cierre(cierre);
    }

    @Transactional(readOnly = true)
    public List<CierreVista> historial(Integer supervisorId) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        return cierres.findAllByOrderByFechaHoraDesc().stream().map(Resumenes::cierre).toList();
    }
}
