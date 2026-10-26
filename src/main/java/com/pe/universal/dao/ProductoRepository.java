package com.pe.universal.dao;

import com.pe.universal.model.Producto;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    @Query("select p from Producto p where p.estado = :activo "
            + "and (:categoria is null or p.categoria.idCategoria = :categoria) "
            + "and (:nombre = '' or lower(p.nombre) like lower(concat('%', :nombre, '%'))) "
            + "order by p.nombre")
    List<Producto> buscarCatalogo(@Param("nombre") String nombre,
            @Param("categoria") Short categoria, @Param("activo") Byte activo);

    boolean existsByNombreIgnoreCase(String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.idProducto = :id")
    Optional<Producto> bloquear(@Param("id") Integer id);

    @Query("select p from Producto p where p.estado = :activo "
            + "and p.stockActual <= p.stockMinimo order by p.nombre")
    List<Producto> bajos(@Param("activo") Byte activo);
}
