package com.pe.universal.service;

import com.pe.universal.datos.Resumenes.CategoriaVista;
import com.pe.universal.datos.Resumenes.ProductoVista;
import com.pe.universal.manager.ProductoManager;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductoService {
    private final ProductoManager gestor;
    public ProductoService(ProductoManager gestor) { this.gestor = gestor; }

    public List<ProductoVista> buscarProductos(String nombre, Short categoriaId) {
        return gestor.buscar(nombre, categoriaId);
    }
    public ProductoVista consultarProducto(Integer productoId) { return gestor.consultar(productoId); }
    public List<CategoriaVista> listarCategorias() { return gestor.listarCategorias(); }
    public List<ProductoVista> listarProductosSupervisor(Integer supervisorId) {
        return gestor.listarSupervisor(supervisorId);
    }
    public ProductoVista consultarProductoSupervisor(Integer supervisorId, Integer productoId) {
        return gestor.consultarSupervisor(supervisorId, productoId);
    }
    public CategoriaVista crearCategoria(Integer supervisorId, String nombre, String descripcion) {
        return gestor.crearCategoria(supervisorId, nombre, descripcion);
    }
    public ProductoVista registrarProducto(Integer supervisorId, Short categoriaId, String nombre,
            String descripcion, BigDecimal precio, Integer stockMinimo) {
        return gestor.registrar(supervisorId, categoriaId, nombre, descripcion, precio, stockMinimo);
    }
    public ProductoVista modificarProducto(Integer supervisorId, Integer productoId, Short categoriaId,
            String nombre, String descripcion, BigDecimal precio, Integer stockMinimo) {
        return gestor.modificar(supervisorId, productoId, categoriaId, nombre, descripcion, precio, stockMinimo);
    }
    public void retirarProducto(Integer supervisorId, Integer productoId) {
        gestor.retirar(supervisorId, productoId);
    }
}
