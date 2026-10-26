package com.pe.universal.service;

import com.pe.universal.datos.Resumenes.MovimientoVista;
import com.pe.universal.datos.Resumenes.ProductoVista;
import com.pe.universal.datos.Resumenes.ReporteInventarioVista;
import com.pe.universal.manager.InventarioManager;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InventarioService {
    private final InventarioManager gestor;
    public InventarioService(InventarioManager gestor) { this.gestor = gestor; }

    public MovimientoVista registrarEntrada(Integer supervisorId, Integer productoId, Integer cantidad, String motivo) {
        return gestor.registrarMovimiento(supervisorId, productoId, "ENTRADA", cantidad, motivo);
    }
    public MovimientoVista registrarSalida(Integer supervisorId, Integer productoId, Integer cantidad, String motivo) {
        return gestor.registrarMovimiento(supervisorId, productoId, "SALIDA", cantidad, motivo);
    }
    
    public MovimientoVista ajustarStock(Integer supervisorId, Integer productoId, Integer diferencia, String motivo) {
        return gestor.registrarMovimiento(supervisorId, productoId, "AJUSTE", diferencia, motivo);
    }
    public List<MovimientoVista> historialMovimientos(Integer supervisorId) { return gestor.historial(supervisorId); }
    public List<ProductoVista> productosConStockBajo(Integer supervisorId) { return gestor.alertasStock(supervisorId); }
    public ReporteInventarioVista reporteInventario(Integer supervisorId) { return gestor.reporte(supervisorId); }
}
