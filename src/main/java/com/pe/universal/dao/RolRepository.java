package com.pe.universal.dao;

import com.pe.universal.model.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Byte> {
    Optional<Rol> findByNombreRol(String nombreRol);
}
