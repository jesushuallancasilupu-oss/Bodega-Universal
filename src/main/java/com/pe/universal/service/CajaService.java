package com.pe.universal.service;

import com.pe.universal.datos.Resumenes.CierreVista;
import com.pe.universal.manager.CajaManager;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CajaService {
    private final CajaManager gestor;
    public CajaService(CajaManager gestor) { this.gestor = gestor; }

    public BigDecimal efectivoPendiente(Integer vendedorId) { return gestor.efectivoPendiente(vendedorId); }
    public CierreVista cerrarCaja(Integer vendedorId, BigDecimal montoRegistrado) {
        return gestor.cerrarCaja(vendedorId, montoRegistrado);
    }
    public List<CierreVista> historialCierres(Integer supervisorId) { return gestor.historial(supervisorId); }
}
