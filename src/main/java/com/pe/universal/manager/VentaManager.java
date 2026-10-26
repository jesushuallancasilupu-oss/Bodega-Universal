package com.pe.universal.manager;

import com.pe.universal.dao.*;
import com.pe.universal.datos.LineaProducto;
import com.pe.universal.datos.Resumenes;
import com.pe.universal.datos.Resumenes.*;
import com.pe.universal.exception.NoEncontradoException;
import com.pe.universal.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VentaManager {
    private final VentaRepository ventas;
    private final DetalleVentaRepository detalles;
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detallePedidos;
    private final ProductoRepository productos;
    private final PagoRepository pagos;
    private final ComprobanteRepository comprobantes;
    private final UsuarioRepository usuarios;
    private final UsuarioManager gestorUsuarios;

    public VentaManager(VentaRepository ventas, DetalleVentaRepository detalles, PedidoRepository pedidos,
            DetallePedidoRepository detallePedidos, ProductoRepository productos, PagoRepository pagos,
            ComprobanteRepository comprobantes, UsuarioRepository usuarios, UsuarioManager gestorUsuarios) {
        this.ventas = ventas; this.detalles = detalles; this.pedidos = pedidos;
        this.detallePedidos = detallePedidos; this.productos = productos;
        this.pagos = pagos; this.comprobantes = comprobantes; this.usuarios = usuarios; this.gestorUsuarios = gestorUsuarios;
    }

    @Transactional
    public VentaVista registrarPorPedido(Integer vendedorId, Integer pedidoId) {
        Usuario vendedor = vendedorBloqueado(vendedorId);
        Pedido pedido = pedidos.bloquear(pedidoId)
                .orElseThrow(() -> new NoEncontradoException("Pedido inexistente"));
        Reglas.exigir(pedido.getEstado().equals("LISTO"), "El pedido debe estar LISTO");
        Reglas.exigir(ventas.findByPedidoIdPedido(pedidoId).isEmpty(), "Este pedido ya tiene venta");
        List<DetallePedido> detallesPedido = detallePedidos.findByPedidoIdPedido(pedidoId);
        Reglas.exigir(!detallesPedido.isEmpty(), "Pedido vacío");
        List<LineaPrecio> lineas = detallesPedido.stream()
                .map(detalle -> new LineaPrecio(detalle.getProducto().getIdProducto(), detalle.getCantidad(), detalle.getPrecioUnitario()))
                .toList();
        Venta venta = registrar(vendedor, pedido, "PEDIDO", lineas);
        pedido.setEstado("ENTREGADO"); pedido.setVendedor(vendedor);
        return vista(venta);
    }

    @Transactional
    public VentaVista registrarPresencial(Integer vendedorId, List<LineaProducto> productosVendidos) {
        Usuario vendedor = vendedorBloqueado(vendedorId);
        Reglas.exigir(productosVendidos != null && !productosVendidos.isEmpty(), "Venta sin productos");
        Set<Integer> unicos = new HashSet<>();
        List<LineaPrecio> lineas = new ArrayList<>();
        for (LineaProducto linea : productosVendidos) {
            Reglas.exigir(linea != null && linea.productoId() != null, "Producto inválido");
            Reglas.exigir(unicos.add(linea.productoId()), "No repita productos; sume las cantidades");
            Reglas.exigir(linea.cantidad() != null, "Cantidad requerida"); Reglas.cantidad(linea.cantidad());
            Producto producto = productos.findById(linea.productoId())
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            lineas.add(new LineaPrecio(linea.productoId(), linea.cantidad(), producto.getPrecio()));
        }
        return vista(registrar(vendedor, null, "PRESENCIAL", lineas));
    }

    private record LineaPrecio(Integer productoId, int cantidad, BigDecimal precio) {}

    private Usuario vendedorBloqueado(Integer vendedorId) {
        gestorUsuarios.exigir(vendedorId, "VENDEDOR");
        return usuarios.bloquear(vendedorId)
                .orElseThrow(() -> new NoEncontradoException("Vendedor inexistente"));
    }

    private Venta registrar(Usuario vendedor, Pedido pedido, String tipo, List<LineaPrecio> lineas) {

        Map<Integer, Producto> bloqueados = new HashMap<>();
        for (Integer id : lineas.stream().map(LineaPrecio::productoId).distinct().sorted().toList()) {
            bloqueados.put(id, productos.bloquear(id)
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado")));
        }
        Map<Integer, BigDecimal> precios = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LineaPrecio linea : lineas) {
            Producto producto = bloqueados.get(linea.productoId());
            Reglas.cantidadPorVenta(producto, linea.cantidad());
            BigDecimal precio = tipo.equals("PEDIDO") ? linea.precio() : producto.getPrecio();
            Reglas.positivo(precio);
            Reglas.exigir(producto.getEstado() == 1, "Producto desactivado: " + producto.getNombre());
            Reglas.exigir(producto.getStockActual() >= linea.cantidad(),
                    "Stock insuficiente: " + producto.getNombre());
            producto.setStockActual(producto.getStockActual() - linea.cantidad());
            precios.put(linea.productoId(), precio);
            total = total.add(Reglas.importe(precio, linea.cantidad()));
        }
        Reglas.exigir(total.compareTo(new BigDecimal("99999999.99")) <= 0, "Venta demasiado grande");
        Venta venta = new Venta(); venta.setVendedor(vendedor); venta.setPedido(pedido);
        venta.setTipoVenta(tipo); venta.setSubtotal(total); venta.setTotal(total);
        venta.setFechaHora(LocalDateTime.now()); ventas.save(venta);
        for (LineaPrecio linea : lineas) {
            DetalleVenta detalle = new DetalleVenta(); detalle.setVenta(venta); detalle.setProducto(bloqueados.get(linea.productoId()));
            detalle.setCantidad(linea.cantidad()); detalle.setPrecioUnitario(precios.get(linea.productoId())); detalles.save(detalle);
        }
        detalles.flush();
        return venta;
    }

    private Venta ventaEditableDelVendedor(Integer vendedorId, Integer ventaId) {
        Usuario vendedor = vendedorBloqueado(vendedorId);
        Venta venta = ventas.bloquear(ventaId)
                .orElseThrow(() -> new NoEncontradoException("Venta no encontrada"));
        Reglas.exigir(venta.getVendedor().getIdUsuario().equals(vendedor.getIdUsuario()),
                "La venta corresponde a otro vendedor");
        Reglas.exigir(venta.getCierre() == null, "La venta pertenece a una caja cerrada");
        return venta;
    }

    @Transactional
    public PagoVista registrarPago(Integer vendedorId, Integer ventaId, String metodo, BigDecimal monto) {
        Venta venta = ventaEditableDelVendedor(vendedorId, ventaId);
        Reglas.exigir(!pagos.existsByVentaIdVenta(ventaId), "Esta venta ya tiene pago");
        Reglas.exigir(metodo != null, "Indique el método de pago");
        String metodoNormalizado = metodo.toUpperCase();
        Reglas.exigir(List.of("EFECTIVO", "YAPE").contains(metodoNormalizado), "Método de pago no permitido");
        Reglas.positivo(monto);
        Reglas.exigir(monto.compareTo(venta.getTotal()) >= 0, "Pago insuficiente");
        if (metodoNormalizado.equals("YAPE")) Reglas.exigir(monto.compareTo(venta.getTotal()) == 0,
                "En Yape, el monto debe coincidir con el total");
        Pago pago = new Pago(); pago.setVenta(venta); pago.setMetodoPago(metodoNormalizado); pago.setMontoPagado(monto);
        pago.setVuelto(metodoNormalizado.equals("EFECTIVO") ? monto.subtract(venta.getTotal()) : BigDecimal.ZERO);
        return Resumenes.pago(pagos.save(pago));
    }

    @Transactional
    public ComprobanteVista emitirComprobante(Integer vendedorId, Integer ventaId, String tipo) {
        Venta venta = ventaEditableDelVendedor(vendedorId, ventaId);
        Reglas.exigir(pagos.existsByVentaIdVenta(ventaId), "Primero registre el pago");
        Reglas.exigir(comprobantes.findByVentaIdVenta(ventaId).isEmpty(), "Ya existe comprobante");
        Reglas.exigir(tipo != null, "Indique el tipo de comprobante");
        String tipoNormalizado = tipo.toUpperCase();
        Reglas.exigir(List.of("BOLETA", "FACTURA", "TICKET").contains(tipoNormalizado), "Tipo no permitido");
        Comprobante comprobante = new Comprobante(); comprobante.setVenta(venta); comprobante.setTipo(tipoNormalizado);
        comprobante.setNumero(tipoNormalizado.substring(0, 1) + "-" + String.format("%010d", ventaId));
        comprobante.setFechaEmision(LocalDateTime.now());
        return Resumenes.comprobante(comprobantes.save(comprobante));
    }

    @Transactional(readOnly = true)
    public VentaVista consultar(Integer usuarioId, Integer ventaId) {
        Venta venta = ventas.findById(ventaId)
                .orElseThrow(() -> new NoEncontradoException("Venta inexistente"));
        Usuario usuario = gestorUsuarios.exigir(usuarioId, "CLIENTE", "VENDEDOR", "SUPERVISOR");
        String rol = usuario.getRol().getNombreRol();
        if (rol.equals("CLIENTE"))
            Reglas.exigir(venta.getPedido() != null && venta.getPedido().getCliente().getIdUsuario().equals(usuarioId),
                    "La venta no corresponde a este cliente");
        if (rol.equals("VENDEDOR"))
            Reglas.exigir(venta.getVendedor().getIdUsuario().equals(usuarioId), "Venta de otro vendedor");
        return vista(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaVista> historialCliente(Integer clienteId) {
        gestorUsuarios.exigir(clienteId, "CLIENTE");
        return ventas.findByPedidoClienteIdUsuarioOrderByFechaHoraDesc(clienteId)
                .stream().map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    public List<VentaVista> consultarPorFecha(Integer usuarioId, LocalDateTime desde, LocalDateTime hasta) {
        Usuario usuario = gestorUsuarios.exigir(usuarioId, "VENDEDOR", "SUPERVISOR");
        Reglas.exigir(desde != null && hasta != null && hasta.isAfter(desde), "Rango de fechas inválido");
        boolean supervisor = usuario.getRol().getNombreRol().equals("SUPERVISOR");
        return ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(desde, hasta)
                .stream().filter(venta -> supervisor || venta.getVendedor().getIdUsuario().equals(usuarioId))
                .map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    public PagoVista consultarPago(Integer usuarioId, Integer ventaId) {
        consultar(usuarioId, ventaId);
        return Resumenes.pago(pagos.findByVentaIdVenta(ventaId)
                .orElseThrow(() -> new NoEncontradoException("No existe pago")));
    }

    @Transactional(readOnly = true)
    public ComprobanteVista consultarComprobante(Integer usuarioId, Integer ventaId) {
        consultar(usuarioId, ventaId);
        return Resumenes.comprobante(comprobantes.findByVentaIdVenta(ventaId)
                .orElseThrow(() -> new NoEncontradoException("No existe comprobante")));
    }

    @Transactional(readOnly = true)
    public ReporteVentasVista reporteVentas(Integer supervisorId, LocalDate desde, LocalDate hasta) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        Reglas.exigir(desde != null && hasta != null && !hasta.isBefore(desde), "Fechas inválidas");
        List<Venta> lista = ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()).stream()
                .filter(venta -> pagos.existsByVentaIdVenta(venta.getIdVenta())).toList();
        BigDecimal total = lista.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReporteVentasVista(desde, hasta, lista.size(), total,
                lista.stream().map(this::vista).toList());
    }

    public VentaVista vista(Venta venta) {
        List<LineaVista> lineasVenta = detalles.findByVentaIdVenta(venta.getIdVenta()).stream()
                .map(detalle -> new LineaVista(detalle.getProducto().getIdProducto(), detalle.getProducto().getNombre(),
                        detalle.getCantidad(), detalle.getPrecioUnitario(),
                        Reglas.importe(detalle.getPrecioUnitario(), detalle.getCantidad())))
                .toList();
        return new VentaVista(venta.getIdVenta(), venta.getTipoVenta(), venta.getFechaHora(),
                venta.getVendedor().getIdUsuario(), venta.getPedido() == null ? null : venta.getPedido().getIdPedido(),
                venta.getSubtotal(), venta.getTotal(), pagos.existsByVentaIdVenta(venta.getIdVenta()), venta.getCierre() != null, lineasVenta);
    }
}
