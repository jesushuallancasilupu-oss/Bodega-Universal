package com.pe.universal.controller;

import com.pe.universal.datos.Resumenes.PedidoVista;
import com.pe.universal.datos.Resumenes.LineaVista;
import com.pe.universal.exception.NegocioException;
import com.pe.universal.exception.NoEncontradoException;
import com.pe.universal.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cliente")
public class ClienteController {
    private final PedidoService pedidos;
    public ClienteController(PedidoService pedidos) { this.pedidos = pedidos; }

    private PedidoVista borrador(int cliente) {
        List<PedidoVista> borradores = pedidos.recuperarBorradores(cliente);
        return borradores.isEmpty() ? null : borradores.get(0);
    }

    @GetMapping("/carrito") 
    public String carrito(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Tu carrito");
        modelo.addAttribute("pedido", borrador(AccesoWeb.id(sesion)));
        return "cliente/carrito";
    }

    private int unidades(PedidoVista pedido) {
        return pedido == null ? 0 : pedido.lineas().stream()
                .mapToInt(LineaVista::cantidad).sum();
    }

    @GetMapping("/carrito/contador")
    @ResponseBody
    public Map<String, Integer> contador(HttpSession sesion) {
        return Map.of("cantidad", unidades(borrador(AccesoWeb.id(sesion))));
    }

    private PedidoVista agregarAlBorrador(HttpSession sesion, int productoId, int cantidad) {
        int cliente = AccesoWeb.id(sesion);
        PedidoVista pedidoBorrador = borrador(cliente);
        if (pedidoBorrador == null) pedidoBorrador = pedidos.crearPedido(cliente, null);
        return pedidos.agregarProducto(cliente, pedidoBorrador.id(), productoId, cantidad);
    }

    @PostMapping(value="/carrito/agregar", headers="X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> agregarSinRecargarPagina(HttpSession sesion,
            @RequestParam int productoId, @RequestParam(defaultValue="1") int cantidad) {
        try {
            PedidoVista actualizado = agregarAlBorrador(sesion, productoId, cantidad);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "cantidad", unidades(actualizado),
                    "mensaje", "Producto agregado al carrito"));
        } catch (NegocioException | NoEncontradoException | IllegalArgumentException errorCapturado) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false, "mensaje", errorCapturado.getMessage()));
        }
    }

    @PostMapping("/carrito/agregar")
    public String agregar(HttpSession sesion,
            @RequestParam int productoId, @RequestParam(defaultValue="1") int cantidad,
            RedirectAttributes mensajes) {
        agregarAlBorrador(sesion, productoId, cantidad);
        mensajes.addFlashAttribute("exito", "Producto añadido al carrito");
        return "redirect:/cliente/carrito";
    }

    @PostMapping("/carrito/cantidad") 
    public String cantidad(HttpSession sesion,
            @RequestParam int productoId, @RequestParam int cantidad) {
        int cliente = AccesoWeb.id(sesion);
        PedidoVista pedidoBorrador = borrador(cliente);
        if (pedidoBorrador == null) throw new IllegalArgumentException("No hay carrito activo");
        if (cantidad == 0) pedidos.quitarProducto(cliente, pedidoBorrador.id(), productoId);
        else pedidos.cambiarCantidad(cliente, pedidoBorrador.id(), productoId, cantidad);
        return "redirect:/cliente/carrito";
    }

    @GetMapping("/checkout") 
    public String mostrarResumenCompra(HttpSession sesion, Model modelo) {
        PedidoVista pedido = borrador(AccesoWeb.id(sesion));
        if (pedido == null || pedido.lineas().isEmpty()) return "redirect:/cliente/carrito";
        modelo.addAttribute("titulo", "Confirmar pedido");
        modelo.addAttribute("pedido", pedido);
        return "cliente/checkout";
    }

    @PostMapping("/checkout") 
    public String confirmar(HttpSession sesion,
            @RequestParam String horaRecojo, RedirectAttributes mensajes) {
        int cliente = AccesoWeb.id(sesion);
        PedidoVista pedido = borrador(cliente);
        if (pedido == null) throw new IllegalArgumentException("No hay carrito activo");
        pedidos.programarRecojo(cliente, pedido.id(), LocalDateTime.parse(horaRecojo));
        pedidos.confirmarPedido(cliente, pedido.id());
        mensajes.addFlashAttribute("exito", "Pedido confirmado. Paga al recogerlo en la tienda.");
        return "redirect:/cliente/confirmacion/" + pedido.id();
    }

    @GetMapping("/confirmacion/{id}") 
    public String confirmacion(HttpSession sesion,
            @PathVariable int id, Model modelo) {
        PedidoVista pedido = pedidos.consultarPedido(AccesoWeb.id(sesion), id);
        modelo.addAttribute("titulo", "Pedido confirmado"); modelo.addAttribute("pedido", pedido);
        return "cliente/confirmacion";
    }

    @GetMapping("/pedidos") 
    public String historial(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Mis pedidos");
        modelo.addAttribute("pedidos", pedidos.historialPedidos(AccesoWeb.id(sesion)).stream()
                .filter(pedido -> !"BORRADOR".equals(pedido.estado())).toList());
        return "cliente/historial";
    }

    @GetMapping("/pedidos/{id}") 
    public String detalle(HttpSession sesion,
            @PathVariable int id, Model modelo) {
        modelo.addAttribute("titulo", "Detalle de pedido");
        modelo.addAttribute("pedido", pedidos.consultarPedido(AccesoWeb.id(sesion), id));
        return "cliente/pedido";
    }

    @PostMapping("/pedidos/{id}/cancelar") 
    public String cancelar(HttpSession sesion, @PathVariable int id,
            RedirectAttributes mensajes) {
        pedidos.cancelarPedido(AccesoWeb.id(sesion), id);
        mensajes.addFlashAttribute("exito", "Pedido cancelado");
        return "redirect:/cliente/pedidos";
    }

    @GetMapping("/cuenta") 
    public String cuenta(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Mi cuenta");
        modelo.addAttribute("pedidos", pedidos.historialPedidos(AccesoWeb.id(sesion)).size());
        return "cliente/cuenta";
    }
}
