package com.pe.universal.dao;

import com.pe.universal.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Short> {
    boolean existsByNombreIgnoreCase(String nombre);
}
