package com.banco.modelo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** MODELO - Acceso a datos de cuentas. */
public interface CuentaRepository extends JpaRepository<Cuenta, String> {

    List<Cuenta> findByOrderByNumeroAsc();
}
