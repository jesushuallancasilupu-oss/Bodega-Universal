package com.pe.universal.dao;

import com.pe.universal.model.Pago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    boolean existsByVentaIdVenta(Integer ventaId);
    Optional<Pago> findByVentaIdVenta(Integer ventaId);
}
