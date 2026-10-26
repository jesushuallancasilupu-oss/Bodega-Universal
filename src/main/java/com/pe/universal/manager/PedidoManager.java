package com.pe.universal.manager;

import com.pe.universal.dao.*;
import com.pe.universal.datos.Resumenes.LineaVista;
import com.pe.universal.datos.Resumenes.PedidoVista;
import com.pe.universal.exception.NoEncontradoException;
import com.pe.universal.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PedidoManager {
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final UsuarioManager gestorUsuarios;

    public PedidoManager(PedidoRepository pedidos, DetallePedidoRepository detalles, ProductoRepository productos,
            VentaRepository ventas, UsuarioManager gestorUsuarios) {
        this.pedidos = pedidos; this.detalles = detalles; this.productos = productos;
        this.ventas = ventas; this.gestorUsuarios = gestorUsuarios;
    }

    @Transactional
    public PedidoVista crear(Integer clienteId, LocalDateTime horaRecojo) {
        Usuario cliente = gestorUsuarios.exigir(clienteId, "CLIENTE");
        if (horaRecojo != null)
            Reglas.exigir(horaRecojo.isAfter(LocalDateTime.now()), "La hora de recojo debe ser futura");
        Pedido pedido = new Pedido(); pedido.setCliente(cliente); pedido.setEstado("BORRADOR");
        pedido.setHoraRecojo(horaRecojo); pedido.setFechaPedido(LocalDateTime.now());
        pedido.setTotal(BigDecimal.ZERO);
        return vista(pedidos.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoVista> listarDelCliente(Integer clienteId) {
        gestorUsuarios.exigir(clienteId, "CLIENTE");
        return pedidos.findByClienteIdUsuarioOrderByFechaPedidoDesc(clienteId)
                .stream().map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoVista> recuperarBorradores(Integer clienteId) {
        return listarDelCliente(clienteId).stream()
                .filter(pedido -> pedido.estado().equals("BORRADOR")).toList();
    }

    @Transactional(readOnly = true)
    public PedidoVista consultar(Integer clienteId, Integer pedidoId) {
        Pedido pedido = buscar(pedidoId); gestorUsuarios.propietario(clienteId, pedido.getCliente()); return vista(pedido);
    }

    @Transactional
    public PedidoVista agregarProducto(Integer clienteId, Integer pedidoId, Integer productoId, int cantidad) {
        Pedido pedido = editable(clienteId, pedidoId); Reglas.cantidad(cantidad);
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
        Reglas.exigir(producto.getEstado() == 1, "Producto desactivado");
        Reglas.cantidadPorVenta(producto, cantidad);
        var anterior = detalles.findByPedidoIdPedidoAndProductoIdProducto(pedidoId, productoId);
        if (anterior.isPresent()) {
            DetallePedido detalle = anterior.get();
            long nueva = (long) detalle.getCantidad() + cantidad;
            Reglas.exigir(nueva <= Integer.MAX_VALUE, "Cantidad demasiado alta");
            Reglas.cantidadPorVenta(producto, (int) nueva);
            detalle.setCantidad((int) nueva);
        } else {
            DetallePedido detalle = new DetallePedido(); detalle.setPedido(pedido); detalle.setProducto(producto);
            detalle.setCantidad(cantidad); detalle.setPrecioUnitario(producto.getPrecio()); detalles.save(detalle);
        }
        detalles.flush(); recalcular(pedido); return vista(pedido);
    }

    @Transactional
    public PedidoVista cambiarCantidad(Integer clienteId, Integer pedidoId, Integer productoId, int cantidad) {
        Pedido pedido = editable(clienteId, pedidoId); Reglas.cantidad(cantidad);
        DetallePedido detalle = detalles.findByPedidoIdPedidoAndProductoIdProducto(pedidoId, productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no agregado al pedido"));
        Reglas.cantidadPorVenta(detalle.getProducto(), cantidad);
        detalle.setCantidad(cantidad); detalles.flush(); recalcular(pedido); return vista(pedido);
    }

    @Transactional
    public PedidoVista quitarProducto(Integer clienteId, Integer pedidoId, Integer productoId) {
        Pedido pedido = editable(clienteId, pedidoId);
        DetallePedido detalle = detalles.findByPedidoIdPedidoAndProductoIdProducto(pedidoId, productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no agregado al pedido"));
        detalles.delete(detalle); detalles.flush(); recalcular(pedido); return vista(pedido);
    }

    @Transactional
    public PedidoVista programarRecojo(Integer clienteId, Integer pedidoId, LocalDateTime horaRecojo) {
        Pedido pedido = editable(clienteId, pedidoId);
        Reglas.exigir(horaRecojo != null && horaRecojo.isAfter(LocalDateTime.now()),
                "Selecciona una fecha y hora de recojo futuras");
        pedido.setHoraRecojo(horaRecojo);
        return vista(pedido);
    }

    @Transactional
    public PedidoVista confirmar(Integer clienteId, Integer pedidoId) {
        Pedido pedido = editable(clienteId, pedidoId);
        Reglas.exigir(pedido.getHoraRecojo() != null && pedido.getHoraRecojo().isAfter(LocalDateTime.now()),
                "Selecciona una fecha y hora de recojo futuras");
        List<DetallePedido> lista = detalles.findByPedidoIdPedido(pedidoId);
        Reglas.exigir(!lista.isEmpty(), "No puede confirmar un pedido vacío");
        for (DetallePedido detalle : lista) {
            Producto producto = productos.findById(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new NoEncontradoException("Producto inexistente"));
            Reglas.cantidadPorVenta(producto, detalle.getCantidad());
            Reglas.exigir(producto.getEstado() == 1 && producto.getStockActual() >= detalle.getCantidad(),
                    "Stock insuficiente: " + producto.getNombre());
        }
        recalcular(pedido);
        pedido.setEstado("PENDIENTE"); 
        return vista(pedido);
    }

    @Transactional
    public PedidoVista cancelar(Integer clienteId, Integer pedidoId) {
        Pedido pedido = editableCancelable(clienteId, pedidoId);
        Reglas.exigir(ventas.findByPedidoIdPedido(pedidoId).isEmpty(), "El pedido ya fue vendido");
        pedido.setEstado("CANCELADO"); return vista(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoVista> consultarPedidos(Integer vendedorId, String estado) {
        gestorUsuarios.exigir(vendedorId, "VENDEDOR", "SUPERVISOR");
        List<Pedido> pedidosConsultados = estado == null || estado.isBlank()
                ? pedidos.findAll() : pedidos.findByEstadoOrderByFechaPedidoAsc(estado.toUpperCase());
        return pedidosConsultados.stream().map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    public PedidoVista consultarParaVendedor(Integer vendedorId, Integer pedidoId) {
        gestorUsuarios.exigir(vendedorId, "VENDEDOR", "SUPERVISOR");
        return vista(buscar(pedidoId));
    }

    @Transactional
    public PedidoVista cambiarEstado(Integer vendedorId, Integer pedidoId, String siguiente) {
        gestorUsuarios.exigir(vendedorId, "VENDEDOR", "SUPERVISOR");
        Reglas.exigir(siguiente != null, "Debe indicar el nuevo estado");
        Pedido pedido = pedidos.bloquear(pedidoId)
                .orElseThrow(() -> new NoEncontradoException("Pedido no encontrado"));
        String nuevo = siguiente.toUpperCase();
        Reglas.transicion(pedido.getEstado(), nuevo);
        Reglas.exigir(!nuevo.equals("ENTREGADO"),
                "ENTREGADO se establece al registrar la venta del pedido");
        pedido.setEstado(nuevo); return vista(pedido);
    }

    private Pedido editable(Integer clienteId, Integer pedidoId) {
        Pedido pedido = pedidos.bloquear(pedidoId)
                .orElseThrow(() -> new NoEncontradoException("Pedido no encontrado"));
        gestorUsuarios.exigir(clienteId, "CLIENTE"); gestorUsuarios.propietario(clienteId, pedido.getCliente());
        Reglas.exigir(pedido.getEstado().equals("BORRADOR"), "Solo se modifican pedidos en BORRADOR");
        return pedido;
    }

    private Pedido editableCancelable(Integer clienteId, Integer pedidoId) {
        Pedido pedido = pedidos.bloquear(pedidoId)
                .orElseThrow(() -> new NoEncontradoException("Pedido no encontrado"));
        gestorUsuarios.exigir(clienteId, "CLIENTE"); gestorUsuarios.propietario(clienteId, pedido.getCliente());
        Reglas.exigir(List.of("BORRADOR", "PENDIENTE").contains(pedido.getEstado()),
                "Este pedido ya no puede cancelarse");
        return pedido;
    }

    private Pedido buscar(Integer id) {
        return pedidos.findById(id).orElseThrow(() -> new NoEncontradoException("Pedido no encontrado"));
    }

    private void recalcular(Pedido pedido) {
        BigDecimal total = detalles.findByPedidoIdPedido(pedido.getIdPedido()).stream()
                .map(detalle -> Reglas.importe(detalle.getPrecioUnitario(), detalle.getCantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Reglas.exigir(total.compareTo(new BigDecimal("99999999.99")) <= 0, "El pedido excede el importe máximo");
        pedido.setTotal(total);
    }

    public PedidoVista vista(Pedido pedido) {
        List<LineaVista> lineas = detalles.findByPedidoIdPedido(pedido.getIdPedido()).stream()
                .map(detalle -> new LineaVista(detalle.getProducto().getIdProducto(), detalle.getProducto().getNombre(),
                        detalle.getCantidad(), detalle.getPrecioUnitario(),
                        Reglas.importe(detalle.getPrecioUnitario(), detalle.getCantidad()),
                        detalle.getProducto().getImagenUrl(), detalle.getProducto().getCantidadPorVenta()))
                .toList();
        return new PedidoVista(pedido.getIdPedido(), pedido.getCliente().getIdUsuario(), pedido.getEstado(),
                pedido.getFechaPedido(), pedido.getHoraRecojo(), pedido.getTotal(), pedido.getTotal(), lineas);
    }
}
