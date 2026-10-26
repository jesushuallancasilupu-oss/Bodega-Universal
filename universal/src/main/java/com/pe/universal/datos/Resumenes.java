package com.pe.universal.datos;

import com.pe.universal.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Resumenes {
    private Resumenes() {}

    public record UsuarioVista(Integer id, String nombres, String apellidos, String correo,
            String rol, Byte estado) {}

    public record CategoriaVista(Short id, String nombre, String descripcion) {}

    public record ProductoVista(Integer id, String nombre, String descripcion, BigDecimal precio,
            Integer stock, Integer stockMinimo, boolean disponible, String categoria, Byte estado,
            String imagenUrl, Integer cantidadPorVenta) {}

    public record LineaVista(Integer productoId, String producto, Integer cantidad,
            BigDecimal precioUnitario, BigDecimal subtotal, String imagenUrl, Integer cantidadPorVenta) {
                
        public LineaVista(Integer productoId, String producto, Integer cantidad,
                BigDecimal precioUnitario, BigDecimal subtotal) {
            this(productoId, producto, cantidad, precioUnitario, subtotal, null, 1);
        }

        public LineaVista(Integer productoId, String producto, Integer cantidad,
                BigDecimal precioUnitario, BigDecimal subtotal, String imagenUrl) {
            this(productoId, producto, cantidad, precioUnitario, subtotal, imagenUrl, 1);
        }
    }

    public record PedidoVista(Integer id, Integer clienteId, String estado, LocalDateTime fechaPedido,
            LocalDateTime horaRecojo, BigDecimal subtotal, BigDecimal total, List<LineaVista> lineas) {}

    public record VentaVista(Integer id, String tipo, LocalDateTime fecha, Integer vendedorId,
            Integer pedidoId, BigDecimal subtotal, BigDecimal total, boolean pagada,
            boolean cerrada, List<LineaVista> lineas) {}

    public record PagoVista(Integer id, Integer ventaId, String metodo, BigDecimal monto, BigDecimal vuelto) {}

    public record ComprobanteVista(Integer id, Integer ventaId, String tipo, String numero,
            LocalDateTime fechaEmision) {}

    public record MovimientoVista(Integer id, Integer productoId, String producto, String tipo,
            Integer cantidad, LocalDateTime fecha, String motivo, Integer supervisorId) {}

    public record CierreVista(Integer id, Integer vendedorId, LocalDateTime fecha,
            BigDecimal esperado, BigDecimal registrado, BigDecimal diferencia) {}

    public record ReporteVentasVista(LocalDate desde, LocalDate hasta, long cantidad,
            BigDecimal total, List<VentaVista> ventas) {}

    public record ReporteInventarioVista(long productos, long bajoMinimo,
            List<ProductoVista> inventario) {}

    public static UsuarioVista usuario(Usuario usuario) {
        return new UsuarioVista(usuario.getIdUsuario(), usuario.getNombres(), usuario.getApellidos(), usuario.getCorreo(),
                usuario.getRol().getNombreRol(), usuario.getEstado());
    }

    public static CategoriaVista categoria(Categoria categoria) {
        return new CategoriaVista(categoria.getIdCategoria(), categoria.getNombre(), categoria.getDescripcion());
    }

    public static ProductoVista producto(Producto producto) {
        return new ProductoVista(producto.getIdProducto(), producto.getNombre(), producto.getDescripcion(), producto.getPrecio(),
                producto.getStockActual(), producto.getStockMinimo(), producto.getEstado() == 1 && producto.getStockActual() >= producto.getCantidadPorVenta(),
                producto.getCategoria().getNombre(), producto.getEstado(), producto.getImagenUrl(), producto.getCantidadPorVenta());
    }

    public static MovimientoVista movimiento(MovimientoInventario movimiento) {
        return new MovimientoVista(movimiento.getIdMovimiento(), movimiento.getProducto().getIdProducto(),
                movimiento.getProducto().getNombre(), movimiento.getTipo(), movimiento.getCantidad(), movimiento.getFechaHora(),
                movimiento.getMotivo(), movimiento.getSupervisor().getIdUsuario());
    }

    public static CierreVista cierre(CierreCaja cierre) {
        return new CierreVista(cierre.getIdCierre(), cierre.getVendedor().getIdUsuario(), cierre.getFechaHora(),
                cierre.getMontoEsperado(), cierre.getMontoRegistrado(),
                cierre.getMontoRegistrado().subtract(cierre.getMontoEsperado()));
    }

    public static PagoVista pago(Pago pago) {
        return new PagoVista(pago.getIdPago(), pago.getVenta().getIdVenta(), pago.getMetodoPago(),
                pago.getMontoPagado(), pago.getVuelto());
    }

    public static ComprobanteVista comprobante(Comprobante comprobante) {
        return new ComprobanteVista(comprobante.getIdComprobante(), comprobante.getVenta().getIdVenta(), comprobante.getTipo(),
                comprobante.getNumero(), comprobante.getFechaEmision());
    }
}
