package com.pe.universal.controller;

import com.pe.universal.datos.LineaProducto;
import com.pe.universal.datos.Resumenes.*;
import com.pe.universal.service.*;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendedor")
public class VendedorController {
    private final PedidoService pedidos;
    private final ProductoService productos;
    private final VentaService ventas;
    private final UsuarioService usuarios;
    private final CajaService caja;
    private final CobroWebService cobro;
    public VendedorController(PedidoService pedidos, ProductoService productos, VentaService ventas,
            UsuarioService usuarios, CajaService caja, CobroWebService cobro) {
        this.pedidos = pedidos; this.productos = productos; this.ventas = ventas;
        this.usuarios = usuarios; this.caja = caja; this.cobro = cobro;
    }

    @GetMapping 
    public String resumen(HttpSession sesion, Model modelo) {
        int id = AccesoWeb.id(sesion);
        List<VentaVista> hoy = ventas.ventasPorFecha(id, LocalDate.now(), LocalDate.now());
        modelo.addAttribute("titulo", "Resumen del vendedor");
        modelo.addAttribute("ventas", hoy);
        modelo.addAttribute("totalHoy", hoy.stream().filter(VentaVista::pagada).map(VentaVista::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        modelo.addAttribute("pendientes", pedidos.pedidosParaVendedor(id, null).stream()
                .filter(pedido -> List.of("PENDIENTE", "EN_PREPARACION", "LISTO").contains(pedido.estado())).toList());
        modelo.addAttribute("alertas", productos.buscarProductos("", null).stream()
                .filter(producto -> producto.stock() <= producto.stockMinimo()).toList());
        return "vendedor/resumen";
    }

    @GetMapping("/pos") 
    public String puntoDeVenta(Model modelo) {
        modelo.addAttribute("titulo", "Punto de venta");
        modelo.addAttribute("productos", productos.buscarProductos("", null));
        return "vendedor/pos";
    }

    @PostMapping("/pos/cobrar") 
    public String cobrar(HttpSession sesion,
            @RequestParam("ids") List<Integer> identificadores, @RequestParam List<Integer> cantidades,
            @RequestParam String metodo, @RequestParam BigDecimal recibido, RedirectAttributes mensajes) {
        if (identificadores.size() != cantidades.size() || identificadores.isEmpty())
            throw new IllegalArgumentException("Revisa los productos de la venta");
        List<LineaProducto> lineasVenta = new ArrayList<>();
        for (int indice = 0; indice < identificadores.size(); indice++) lineasVenta.add(new LineaProducto(identificadores.get(indice), cantidades.get(indice)));
        VentaVista venta = cobro.presencial(AccesoWeb.id(sesion), lineasVenta, metodo, recibido);
        mensajes.addFlashAttribute("exito", "Venta registrada y cobrada correctamente");
        return "redirect:/vendedor/ventas/" + venta.id();
    }

    @GetMapping("/ventas/{id}") 
    public String venta(HttpSession sesion, @PathVariable int id, Model modelo) {
        int vendedor = AccesoWeb.id(sesion);
        VentaVista venta = ventas.consultarVenta(vendedor, id);
        modelo.addAttribute("titulo", "Comprobante de venta");
        modelo.addAttribute("venta", venta);
        if (venta.pagada()) {
            modelo.addAttribute("pago", ventas.consultarPago(vendedor, id));
            modelo.addAttribute("comprobante", ventas.consultarComprobante(vendedor, id));
        }
        return "vendedor/venta";
    }

    @GetMapping("/pedidos") 
    public String pendientes(HttpSession sesion, Model modelo,
            @RequestParam(required=false) String estado) {
        modelo.addAttribute("titulo", "Pedidos por preparar");
        modelo.addAttribute("estado", estado);
        modelo.addAttribute("clientesPorId", usuarios.listarClientes(AccesoWeb.id(sesion)).stream()
                .collect(Collectors.toMap(UsuarioVista::id,
                        cliente -> cliente.nombres() + " " + cliente.apellidos())));
        modelo.addAttribute("pedidos", pedidos.pedidosParaVendedor(AccesoWeb.id(sesion), estado).stream()
                .filter(pedido -> List.of("PENDIENTE", "EN_PREPARACION", "LISTO").contains(pedido.estado())).toList());
        return "vendedor/pedidos";
    }

    @GetMapping("/pedidos/{id}") 
    public String pedido(HttpSession sesion, @PathVariable int id, Model modelo) {
        modelo.addAttribute("titulo", "Preparación de pedido");
        modelo.addAttribute("clientesPorId", usuarios.listarClientes(AccesoWeb.id(sesion)).stream()
                .collect(Collectors.toMap(UsuarioVista::id,
                        cliente -> cliente.nombres() + " " + cliente.apellidos())));
        modelo.addAttribute("pedido", pedidos.consultarPedidoVendedor(AccesoWeb.id(sesion), id));
        return "vendedor/pedido";
    }

    @PostMapping("/pedidos/{id}/estado") 
    public String estado(HttpSession sesion,
            @PathVariable int id, @RequestParam String estado, RedirectAttributes mensajes) {
        pedidos.cambiarEstado(AccesoWeb.id(sesion), id, estado);
        mensajes.addFlashAttribute("exito", "Estado del pedido actualizado");
        return "redirect:/vendedor/pedidos/" + id;
    }

    @PostMapping("/pedidos/{id}/entregar") 
    public String entregar(HttpSession sesion,
            @PathVariable int id, @RequestParam String metodo, @RequestParam BigDecimal recibido,
            RedirectAttributes mensajes) {
        VentaVista venta = cobro.desdePedido(AccesoWeb.id(sesion), id, metodo, recibido);
        mensajes.addFlashAttribute("exito", "Pedido cobrado y entregado correctamente");
        return "redirect:/vendedor/ventas/" + venta.id();
    }

    @GetMapping("/historial") 
    public String historial(HttpSession sesion,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta, Model modelo) {
        if (desde == null) desde = LocalDate.now().minusDays(30);
        if (hasta == null) hasta = LocalDate.now();
        modelo.addAttribute("titulo", "Historial de ventas");
        modelo.addAttribute("desde", desde); modelo.addAttribute("hasta", hasta);
        modelo.addAttribute("ventas", ventas.ventasPorFecha(AccesoWeb.id(sesion), desde, hasta));
        return "vendedor/historial";
    }

    @GetMapping("/clientes") 
    public String clientes(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Consulta de clientes");
        modelo.addAttribute("clientes", usuarios.listarClientes(AccesoWeb.id(sesion)));
        return "vendedor/clientes";
    }

    @GetMapping("/caja") 
    public String cierre(HttpSession sesion, Model modelo) {
        int vendedor = AccesoWeb.id(sesion);
        modelo.addAttribute("titulo", "Cierre de caja");
        modelo.addAttribute("efectivoEsperado", caja.efectivoPendiente(vendedor));
        modelo.addAttribute("ventas", ventas.ventasPorFecha(vendedor, LocalDate.now().minusDays(30), LocalDate.now())
                .stream().filter(venta -> !venta.cerrada()).toList());
        return "vendedor/caja";
    }

    @PostMapping("/caja/cerrar") 
    public String cerrar(HttpSession sesion,
            @RequestParam BigDecimal montoRegistrado, RedirectAttributes mensajes) {
        CierreVista cierre = caja.cerrarCaja(AccesoWeb.id(sesion), montoRegistrado);
        mensajes.addFlashAttribute("exito", "Caja cerrada. Diferencia: S/ " + cierre.diferencia());
        return "redirect:/vendedor/caja";
    }
}
