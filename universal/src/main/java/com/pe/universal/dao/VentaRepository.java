package com.pe.universal.dao;

import com.pe.universal.model.Venta;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.idVenta = :id")
    Optional<Venta> bloquear(@Param("id") Integer id);

    Optional<Venta> findByPedidoIdPedido(Integer pedidoId);
    List<Venta> findByVendedorIdUsuarioAndCierreIsNull(Integer vendedorId);
    List<Venta> findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
            LocalDateTime desde, LocalDateTime hasta);
    List<Venta> findByPedidoClienteIdUsuarioOrderByFechaHoraDesc(Integer clienteId);
}
