package com.pe.universal.service;

import com.pe.universal.datos.Resumenes.PedidoVista;
import com.pe.universal.manager.PedidoManager;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {
    private final PedidoManager gestor;
    public PedidoService(PedidoManager gestor) { this.gestor = gestor; }

    public PedidoVista crearPedido(Integer clienteId, LocalDateTime horaRecojo) {
        return gestor.crear(clienteId, horaRecojo);
    }
    public PedidoVista agregarProducto(Integer clienteId, Integer pedidoId, Integer productoId, int cantidad) {
        return gestor.agregarProducto(clienteId, pedidoId, productoId, cantidad);
    }
    public PedidoVista cambiarCantidad(Integer clienteId, Integer pedidoId, Integer productoId, int cantidad) {
        return gestor.cambiarCantidad(clienteId, pedidoId, productoId, cantidad);
    }
    public PedidoVista quitarProducto(Integer clienteId, Integer pedidoId, Integer productoId) {
        return gestor.quitarProducto(clienteId, pedidoId, productoId);
    }
    public PedidoVista consultarPedido(Integer clienteId, Integer pedidoId) {
        return gestor.consultar(clienteId, pedidoId);
    }
    public PedidoVista programarRecojo(Integer clienteId, Integer pedidoId, LocalDateTime horaRecojo) {
        return gestor.programarRecojo(clienteId, pedidoId, horaRecojo);
    }
    public PedidoVista confirmarPedido(Integer clienteId, Integer pedidoId) {
        return gestor.confirmar(clienteId, pedidoId);
    }
    public PedidoVista cancelarPedido(Integer clienteId, Integer pedidoId) {
        return gestor.cancelar(clienteId, pedidoId);
    }
    public List<PedidoVista> historialPedidos(Integer clienteId) { return gestor.listarDelCliente(clienteId); }
    public List<PedidoVista> recuperarBorradores(Integer clienteId) { return gestor.recuperarBorradores(clienteId); }
    public List<PedidoVista> pedidosParaVendedor(Integer vendedorId, String estado) {
        return gestor.consultarPedidos(vendedorId, estado);
    }
    public PedidoVista consultarPedidoVendedor(Integer vendedorId, Integer pedidoId) {
        return gestor.consultarParaVendedor(vendedorId, pedidoId);
    }
    public PedidoVista cambiarEstado(Integer vendedorId, Integer pedidoId, String estado) {
        return gestor.cambiarEstado(vendedorId, pedidoId, estado);
    }
}
