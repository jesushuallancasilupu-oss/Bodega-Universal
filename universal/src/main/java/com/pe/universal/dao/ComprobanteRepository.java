package com.pe.universal.dao;

import com.pe.universal.model.Comprobante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Integer> {
    Optional<Comprobante> findByVentaIdVenta(Integer ventaId);
}
