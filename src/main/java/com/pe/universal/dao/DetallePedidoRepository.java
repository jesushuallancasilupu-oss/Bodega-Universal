package com.pe.universal.dao;

import com.pe.universal.model.DetallePedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {
    List<DetallePedido> findByPedidoIdPedido(Integer pedidoId);
    Optional<DetallePedido> findByPedidoIdPedidoAndProductoIdProducto(Integer pedidoId, Integer productoId);
}
