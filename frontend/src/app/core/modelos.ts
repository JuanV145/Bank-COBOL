/**
 * Modelos que reflejan la API de Spring Boot.
 */
export interface Cuenta {
  numero: string;
  titular: string;
  tipo: string;
  saldoCents: number;
  estado: string;
  fechaApertura: string;
}

export interface Transaccion {
  id: number;
  referencia: string;
  tipo: string;
  cuentaOrigen: string;
  cuentaDestino: string | null;
  importeCents: number;
  comisionCents: number;
  resultado: string;
  retCode: string;
  mensaje: string;
  fechaHora: string;
}

export interface OperacionRequest {
  cuentaOrigen: string;
  cuentaDestino?: string;
  importe?: number;
  timeoutMs?: number;
  simularDemoraMs?: number;
}

export interface OperacionResponse {
  referencia: string;
  tipo: string;
  resultado: string;
  retCode: string;
  mensaje: string;
  importe: number;
  comision: number;
  saldoOrigenFinal: number | null;
  saldoDestinoFinal: number | null;
  duracionMs: number;
}
