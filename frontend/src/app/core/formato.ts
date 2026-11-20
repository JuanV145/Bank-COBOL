/**
 * Formatea un monto ya expresado en unidades monetarias (pesos).
 * Las respuestas de operaciones ya vienen convertidas por el backend.
 */
export function dinero(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) {
    return '—';
  }
  return valor.toLocaleString('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 2,
  });
}

/**
 * Formatea un monto expresado en centavos.
 * Se usa en cuentas e historial, donde el backend envía los centavos.
 */
export function dineroCents(cents: number | null | undefined): string {
  if (cents === null || cents === undefined) {
    return '—';
  }
  return dinero(cents / 100);
}

export function etiquetaTipo(tipo: string): string {
  const mapa: Record<string, string> = {
    DEPOSITO: 'Depósito',
    RETIRO: 'Retiro',
    TRANSFERENCIA: 'Transferencia',
    CONSULTA_SALDO: 'Consulta de saldo',
    CIERRE_CUENTA: 'Cierre de cuenta',
  };
  return mapa[tipo] ?? tipo;
}
