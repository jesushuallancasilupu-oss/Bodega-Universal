package com.pe.universal.controller;

import com.pe.universal.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PublicoController {
    private final ProductoService productos;
    public PublicoController(ProductoService productos) { this.productos = productos; }

    @GetMapping("/") 
    public String inicio(Model modelo) {
        modelo.addAttribute("titulo", "Inicio");
        modelo.addAttribute("productos", productos.buscarProductos("", null).stream().limit(4).toList());
        modelo.addAttribute("categorias", productos.listarCategorias());
        return "publico/inicio";
    }

    @GetMapping("/productos") 
    public String catalogo(@RequestParam(defaultValue="") String buscar,
            @RequestParam(required=false) Short categoria, Model modelo) {
        modelo.addAttribute("titulo", "Catálogo de productos");
        modelo.addAttribute("productos", productos.buscarProductos(buscar, categoria));
        modelo.addAttribute("categorias", productos.listarCategorias());
        modelo.addAttribute("buscar", buscar);
        modelo.addAttribute("categoriaActiva", categoria);
        return "publico/catalogo";
    }

    @GetMapping("/producto/{id}") 
    public String detalle(@PathVariable int id, Model modelo) {
        modelo.addAttribute("titulo", "Detalle de producto");
        modelo.addAttribute("producto", productos.consultarProducto(id));
        return "publico/detalle";
    }

    @GetMapping("/nosotros") 
    public String nosotros(Model modelo) {
        modelo.addAttribute("titulo", "Nosotros"); return "publico/nosotros";
    }
    @GetMapping("/contacto") 
    public String contacto(Model modelo) {
        modelo.addAttribute("titulo", "Contacto"); return "publico/contacto";
    }
}
