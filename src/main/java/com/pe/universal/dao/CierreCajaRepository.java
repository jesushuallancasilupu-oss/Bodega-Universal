package com.pe.universal.dao;

import com.pe.universal.model.CierreCaja;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Integer> {
    List<CierreCaja> findAllByOrderByFechaHoraDesc();
}
