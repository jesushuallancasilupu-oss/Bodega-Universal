package com.pe.universal.manager;

import com.pe.universal.dao.CategoriaRepository;
import com.pe.universal.dao.ProductoRepository;
import com.pe.universal.datos.Resumenes;
import com.pe.universal.datos.Resumenes.CategoriaVista;
import com.pe.universal.datos.Resumenes.ProductoVista;
import com.pe.universal.exception.NoEncontradoException;
import com.pe.universal.model.Categoria;
import com.pe.universal.model.Producto;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductoManager {
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final UsuarioManager gestorUsuarios;
    public ProductoManager(ProductoRepository productos, CategoriaRepository categorias, UsuarioManager gestorUsuarios) {
        this.productos = productos; this.categorias = categorias; this.gestorUsuarios = gestorUsuarios;
    }

    @Transactional(readOnly = true)
    public List<ProductoVista> buscar(String nombre, Short categoriaId) {
        return productos.buscarCatalogo(nombre == null ? "" : nombre.trim(), categoriaId, (byte) 1)
                .stream().map(Resumenes::producto).toList();
    }

    @Transactional(readOnly = true)
    public ProductoVista consultar(Integer productoId) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
        Reglas.exigir(producto.getEstado() == 1, "Producto no disponible");
        return Resumenes.producto(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoVista> listarSupervisor(Integer supervisorId) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        return productos.findAll().stream().map(Resumenes::producto).toList();
    }

    @Transactional(readOnly = true)
    public ProductoVista consultarSupervisor(Integer supervisorId, Integer productoId) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        return Resumenes.producto(productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado")));
    }

    @Transactional(readOnly = true)
    public List<CategoriaVista> listarCategorias() {
        return categorias.findAll().stream().map(Resumenes::categoria).toList();
    }

    @Transactional
    public CategoriaVista crearCategoria(Integer supervisorId, String nombre, String descripcion) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        validarNombre(nombre, 60);
        Reglas.exigir(!categorias.existsByNombreIgnoreCase(nombre.trim()), "Categoría duplicada");
        Categoria categoriaNueva = new Categoria(); categoriaNueva.setNombre(nombre.trim()); categoriaNueva.setDescripcion(descripcion);
        return Resumenes.categoria(categorias.save(categoriaNueva));
    }

    @Transactional
    public ProductoVista registrar(Integer supervisorId, Short categoriaId, String nombre, String descripcion,
            BigDecimal precio, Integer stockMinimo) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        validarProducto(nombre, precio, stockMinimo);
        Reglas.exigir(!productos.existsByNombreIgnoreCase(nombre.trim()), "Ya existe ese producto");
        Producto producto = new Producto();
        producto.setCategoria(categoria(categoriaId)); producto.setNombre(nombre.trim()); producto.setDescripcion(descripcion);
        producto.setPrecio(precio); producto.setCantidadPorVenta(1); producto.setStockActual(0); producto.setStockMinimo(stockMinimo); producto.setEstado((byte) 1);
        return Resumenes.producto(productos.save(producto));
    }

    @Transactional
    public ProductoVista modificar(Integer supervisorId, Integer productoId, Short categoriaId,
            String nombre, String descripcion, BigDecimal precio, Integer stockMinimo) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        validarProducto(nombre, precio, stockMinimo);
        Producto producto = productos.bloquear(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
        Reglas.exigir(producto.getEstado() == 1, "Producto inactivo");
        if (!producto.getNombre().equalsIgnoreCase(nombre.trim()))
            Reglas.exigir(!productos.existsByNombreIgnoreCase(nombre.trim()), "Ya existe ese producto");
        producto.setCategoria(categoria(categoriaId)); producto.setNombre(nombre.trim()); producto.setDescripcion(descripcion);
        if (producto.getCantidadPorVenta() == 2)
            Reglas.exigir(precio.compareTo(new BigDecimal("0.25")) == 0,
                    "Los panes vendidos de dos en dos deben costar S/ 0.25 por unidad");
        producto.setPrecio(precio); producto.setStockMinimo(stockMinimo);
        return Resumenes.producto(producto);
    }

    @Transactional
    public void retirar(Integer supervisorId, Integer productoId) {
        gestorUsuarios.exigir(supervisorId, "SUPERVISOR");
        Producto producto = productos.bloquear(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
        producto.setEstado((byte) 0);
    }

    private Categoria categoria(Short id) {
        return categorias.findById(id).orElseThrow(() -> new NoEncontradoException("Categoría no encontrada"));
    }
    private void validarNombre(String nombre, int maximo) {
        Reglas.exigir(nombre != null && !nombre.isBlank() && nombre.trim().length() <= maximo, "Nombre inválido");
    }
    private void validarProducto(String nombre, BigDecimal precio, Integer stockMinimo) {
        validarNombre(nombre, 100);
        Reglas.positivo(precio);
        Reglas.exigir(stockMinimo != null && stockMinimo >= 0, "Stock mínimo inválido");
    }
}
