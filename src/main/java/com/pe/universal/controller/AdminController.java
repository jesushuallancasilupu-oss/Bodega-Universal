package com.pe.universal.controller;

import com.pe.universal.datos.Resumenes.*;
import com.pe.universal.service.*;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ProductoService productos;
    private final InventarioService inventario;
    private final UsuarioService usuarios;
    private final PedidoService pedidos;
    private final VentaService ventas;
    private final CajaService caja;
    public AdminController(ProductoService productos, InventarioService inventario,
            UsuarioService usuarios, PedidoService pedidos, VentaService ventas, CajaService caja) {
        this.productos=productos; this.inventario=inventario; this.usuarios=usuarios;
        this.pedidos=pedidos; this.ventas=ventas; this.caja=caja;
    }

    @GetMapping 
    public String resumen(HttpSession sesion, Model modelo) {
        int id = AccesoWeb.id(sesion);
        modelo.addAttribute("titulo", "Resumen ejecutivo");
        modelo.addAttribute("reporte", ventas.reporteVentas(id, LocalDate.now(), LocalDate.now()));
        modelo.addAttribute("alertas", inventario.productosConStockBajo(id));
        modelo.addAttribute("inventario", inventario.reporteInventario(id));
        modelo.addAttribute("pedidos", pedidos.pedidosParaVendedor(id, null).stream()
                .filter(producto -> java.util.List.of("PENDIENTE", "EN_PREPARACION", "LISTO").contains(producto.estado())).toList());
        return "admin/resumen";
    }

    @GetMapping("/categorias") 
    public String categorias(Model modelo) {
        modelo.addAttribute("titulo", "Categorías");
        modelo.addAttribute("categorias", productos.listarCategorias());
        return "admin/categorias";
    }

    @PostMapping("/categorias") 
    public String categoriaNueva(HttpSession sesion,
            @RequestParam String nombre, @RequestParam(required=false) String descripcion,
            RedirectAttributes mensajes) {
        productos.crearCategoria(AccesoWeb.id(sesion), nombre, descripcion);
        mensajes.addFlashAttribute("exito", "Categoría creada correctamente");
        return "redirect:/admin/categorias";
    }

    @GetMapping("/productos") 
    public String productos(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Gestión de productos");
        modelo.addAttribute("productos", productos.listarProductosSupervisor(AccesoWeb.id(sesion)));
        return "admin/productos";
    }

    @GetMapping("/productos/nuevo") 
    public String nuevo(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Nuevo producto");
        modelo.addAttribute("categorias", productos.listarCategorias());
        modelo.addAttribute("producto", null);
        return "admin/producto-form";
    }

    @PostMapping("/productos/nuevo") 
    public String registrarProducto(HttpSession sesion, @RequestParam Short categoriaId,
            @RequestParam String nombre, @RequestParam(required=false) String descripcion,
            @RequestParam BigDecimal precio, @RequestParam Integer stockMinimo, RedirectAttributes mensajes) {
        ProductoVista producto=productos.registrarProducto(AccesoWeb.id(sesion),categoriaId,nombre,descripcion,precio,stockMinimo);
        mensajes.addFlashAttribute("exito", "Producto creado. Registra una entrada para agregar stock.");
        return "redirect:/admin/productos/"+producto.id()+"/editar";
    }

    @GetMapping("/productos/{id}/editar") 
    public String editar(HttpSession sesion, @PathVariable int id, Model modelo) {
        modelo.addAttribute("titulo", "Editar producto");
        modelo.addAttribute("producto", productos.consultarProductoSupervisor(AccesoWeb.id(sesion),id));
        modelo.addAttribute("categorias", productos.listarCategorias());
        return "admin/producto-form";
    }

    @PostMapping("/productos/{id}/editar") 
    public String actualizarProducto(HttpSession sesion, @PathVariable int id,
            @RequestParam Short categoriaId, @RequestParam String nombre,
            @RequestParam(required=false) String descripcion, @RequestParam BigDecimal precio,
            @RequestParam Integer stockMinimo, RedirectAttributes mensajes) {
        productos.modificarProducto(AccesoWeb.id(sesion), id, categoriaId, nombre, descripcion, precio, stockMinimo);
        mensajes.addFlashAttribute("exito", "Producto actualizado correctamente");
        return "redirect:/admin/productos/"+id+"/editar";
    }

    @PostMapping("/productos/{id}/retirar") 
    public String retirar(HttpSession sesion,
            @PathVariable int id, RedirectAttributes mensajes) {
        productos.retirarProducto(AccesoWeb.id(sesion), id);
        mensajes.addFlashAttribute("exito", "Producto retirado del catálogo, conservando su historial");
        return "redirect:/admin/productos";
    }

    @GetMapping("/inventario") 
    public String inventario(HttpSession sesion, Model modelo) {
        int id=AccesoWeb.id(sesion);
        modelo.addAttribute("titulo", "Inventario");
        modelo.addAttribute("inventario", inventario.reporteInventario(id));
        modelo.addAttribute("alertas", inventario.productosConStockBajo(id));
        modelo.addAttribute("movimientos", inventario.historialMovimientos(id).stream().limit(12).toList());
        return "admin/inventario";
    }

    @PostMapping("/inventario/movimiento") 
    public String movimiento(HttpSession sesion,
            @RequestParam int productoId, @RequestParam String tipo, @RequestParam int cantidad,
            @RequestParam String motivo, RedirectAttributes mensajes) {
        int id=AccesoWeb.id(sesion);
        switch (tipo) {
            case "ENTRADA" -> inventario.registrarEntrada(id,productoId,cantidad,motivo);
            case "SALIDA" -> inventario.registrarSalida(id,productoId,cantidad,motivo);
            case "AJUSTE" -> inventario.ajustarStock(id,productoId,cantidad,motivo);
            default -> throw new IllegalArgumentException("Movimiento no permitido");
        }
        mensajes.addFlashAttribute("exito", "Movimiento de inventario registrado");
        return "redirect:/admin/inventario";
    }

    @GetMapping("/inventario/movimientos") 
    public String movimientos(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Movimientos de inventario");
        modelo.addAttribute("movimientos", inventario.historialMovimientos(AccesoWeb.id(sesion)));
        return "admin/movimientos";
    }

    @GetMapping("/pedidos") 
    public String pedidos(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Todos los pedidos");
        modelo.addAttribute("clientesPorId", usuarios.listarClientes(AccesoWeb.id(sesion)).stream()
                .collect(Collectors.toMap(UsuarioVista::id, cliente -> cliente.nombres() + " " + cliente.apellidos())));
        modelo.addAttribute("pedidos", pedidos.pedidosParaVendedor(AccesoWeb.id(sesion),null).stream()
                .filter(producto -> !"BORRADOR".equals(producto.estado())).toList());
        return "admin/pedidos";
    }

    @GetMapping("/pedidos/{id}") 
    public String pedidoDetalle(HttpSession sesion,
            @PathVariable int id, Model modelo) {
        modelo.addAttribute("titulo", "Detalle del pedido");
        modelo.addAttribute("pedido", pedidos.consultarPedidoVendedor(AccesoWeb.id(sesion), id));
        return "admin/pedido";
    }

    @GetMapping("/ventas/{id}") 
    public String ventaDetalle(HttpSession sesion,
            @PathVariable int id, Model modelo) {
        int usuario = AccesoWeb.id(sesion);
        VentaVista venta = ventas.consultarVenta(usuario, id);
        modelo.addAttribute("titulo", "Detalle de venta"); modelo.addAttribute("venta", venta);
        if (venta.pagada()) {
            modelo.addAttribute("pago", ventas.consultarPago(usuario, id));
            modelo.addAttribute("comprobante", ventas.consultarComprobante(usuario, id));
        }
        return "admin/venta";
    }

    @GetMapping("/ventas") 
    public String ventas(HttpSession sesion,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta, Model modelo) {
        if(desde==null) desde=LocalDate.now().minusDays(30);
        if(hasta==null) hasta=LocalDate.now();
        modelo.addAttribute("titulo", "Todas las ventas");
        modelo.addAttribute("desde",desde); modelo.addAttribute("hasta",hasta);
        modelo.addAttribute("ventas", ventas.ventasPorFecha(AccesoWeb.id(sesion),desde,hasta));
        return "admin/ventas";
    }

    @GetMapping("/clientes") 
    public String clientes(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Clientes registrados");
        modelo.addAttribute("clientes", usuarios.listarClientes(AccesoWeb.id(sesion)));
        return "admin/clientes";
    }

    @GetMapping("/usuarios") 
    public String usuarios(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Usuarios y vendedores");
        modelo.addAttribute("vendedores", usuarios.listarVendedores(AccesoWeb.id(sesion)));
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/nuevo") 
    public String usuarioNuevo(Model modelo) {
        modelo.addAttribute("titulo", "Crear cuenta de vendedor"); return "admin/usuario-form";
    }

    @PostMapping("/usuarios/nuevo") 
    public String registrarVendedor(HttpSession sesion, @RequestParam String nombres,
            @RequestParam String apellidos, @RequestParam String dni, @RequestParam String telefono,
            @RequestParam String correo, @RequestParam String contrasena, RedirectAttributes mensajes) {
        usuarios.registrarVendedor(AccesoWeb.id(sesion),nombres,apellidos,dni,telefono,correo,contrasena);
        mensajes.addFlashAttribute("exito", "Cuenta de vendedor creada");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/desactivar") 
    public String desactivar(HttpSession sesion,
            @PathVariable int id, RedirectAttributes mensajes) {
        usuarios.desactivarVendedor(AccesoWeb.id(sesion),id);
        mensajes.addFlashAttribute("exito", "Vendedor desactivado");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/cierres") 
    public String cierres(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Historial de cierres de caja");
        modelo.addAttribute("cierres", caja.historialCierres(AccesoWeb.id(sesion)));
        return "admin/cierres";
    }

    @GetMapping("/reportes") 
    public String reportes(HttpSession sesion,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model modelo) {
        if(desde==null) desde=LocalDate.now().minusDays(30);
        if(hasta==null) hasta=LocalDate.now();
        int id=AccesoWeb.id(sesion);
        modelo.addAttribute("titulo", "Reportes del negocio");
        modelo.addAttribute("desde",desde);modelo.addAttribute("hasta",hasta);
        modelo.addAttribute("reporte",ventas.reporteVentas(id,desde,hasta));
        modelo.addAttribute("inventario",inventario.reporteInventario(id));
        return "admin/reportes";
    }
}
