package com.pe.universal.dao;

import com.pe.universal.model.Pedido;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idPedido = :id")
    Optional<Pedido> bloquear(@Param("id") Integer id);

    List<Pedido> findByClienteIdUsuarioOrderByFechaPedidoDesc(Integer clienteId);
    List<Pedido> findByEstadoOrderByFechaPedidoAsc(String estado);
}
