package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.PedidoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoHistorialRepository extends JpaRepository<PedidoHistorial, Long> {

    List<PedidoHistorial> findByPedidoIdOrderByFechaAccionDesc(Long pedidoId);
}
