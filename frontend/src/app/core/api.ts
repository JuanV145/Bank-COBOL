import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Cuenta, OperacionRequest, OperacionResponse, Transaccion } from './modelos';

@Injectable({ providedIn: 'root' })
export class BancoApi {
  private http = inject(HttpClient);
  private base = 'http://localhost:8080/api';

  cuentas(): Observable<Cuenta[]> {
    return this.http.get<Cuenta[]>(`${this.base}/cuentas`);
  }

  crearCuenta(c: { numero: string; titular: string; tipo: string }): Observable<Cuenta> {
    return this.http.post<Cuenta>(`${this.base}/cuentas`, c);
  }

  /** tipo: deposito | retiro | transferencia | saldo | cierre */
  operar(tipo: string, req: OperacionRequest): Observable<OperacionResponse> {
    return this.http.post<OperacionResponse>(`${this.base}/operaciones/${tipo}`, req);
  }

  transacciones(limite = 20): Observable<Transaccion[]> {
    return this.http.get<Transaccion[]>(`${this.base}/transacciones`, {
      params: { limite: limite },
    });
  }
}

/** Extrae un mensaje legible de un error HTTP de la API. */
export function msgError(e: HttpErrorResponse): string {
  const body = e.error as { mensaje?: string; error?: string } | null;
  if (body && typeof body.mensaje === 'string') {
    return body.mensaje;
  }
  if (e.status === 0) {
    return 'No hay conexión con el backend. Verifica que Spring Boot esté corriendo en el puerto 8080.';
  }
  return `Error HTTP ${e.status}: ${e.statusText}`;
}
