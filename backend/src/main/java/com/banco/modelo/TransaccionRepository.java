package com.banco.modelo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** MODELO - Acceso a datos de transacciones. */
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Page<Transaccion> findByOrderByIdDesc(Pageable pageable);

    List<Transaccion> findTop20ByCuentaOrigenOrCuentaDestinoOrderByIdDesc(String origen, String destino);
}
