import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BancoApi, msgError } from '../../core/api';
import { Cuenta, OperacionResponse, Transaccion } from '../../core/modelos';
import { dinero, dineroCents, etiquetaTipo } from '../../core/formato';

interface TipoOp {
  clave: string;
  etiqueta: string;
  descripcion: string;
}

@Component({
  selector: 'app-operaciones-page',
  imports: [FormsModule],
  templateUrl: './operaciones.html',
  styleUrl: './operaciones.css',
})
export class OperacionesPage implements OnInit {
  private api = inject(BancoApi);

  tipos: TipoOp[] = [
    {
      clave: 'deposito',
      etiqueta: 'Depósito',
      descripcion: 'Abona un importe en la cuenta (PGDEPOSIT)',
    },
    {
      clave: 'retiro',
      etiqueta: 'Retiro',
      descripcion: 'Descuenta fondos si hay saldo (PGRETIRO)',
    },
    {
      clave: 'transferencia',
      etiqueta: 'Transferencia',
      descripcion: 'Mueve fondos con comisión 1% (PGTRAN)',
    },
    {
      clave: 'saldo',
      etiqueta: 'Consulta saldo',
      descripcion: 'Consulta el saldo disponible (PGSALDO)',
    },
    {
      clave: 'cierre',
      etiqueta: 'Cierre',
      descripcion: 'Cierra una cuenta en saldo cero (PGCIERRE)',
    },
  ];

  timeouts = [
    { valor: 1000, etiqueta: '1 segundo' },
    { valor: 3000, etiqueta: '3 segundos (default)' },
    { valor: 5000, etiqueta: '5 segundos' },
    { valor: 10000, etiqueta: '10 segundos' },
  ];

  tipo = signal('deposito');
  cuentas = signal<Cuenta[]>([]);
  historial = signal<Transaccion[]>([]);
  resultado = signal<OperacionResponse | null>(null);
  error = signal<string | null>(null);
  ocupado = signal(false);

  cuentaOrigen = '';
  cuentaDestino = '';
  importe: number | null = null;
  timeoutMs = 3000;
  simularDemora = false;

  necesitaImporte = computed(() => ['deposito', 'retiro', 'transferencia'].includes(this.tipo()));
  necesitaDestino = computed(() => this.tipo() === 'transferencia');
  descripcion = computed(() => this.tipos.find((t) => t.clave === this.tipo())?.descripcion ?? '');

  ngOnInit(): void {
    this.cargarCuentas();
    this.cargarHistorial();
  }

  cargarCuentas(): void {
    this.api.cuentas().subscribe({
      next: (cs) => this.cuentas.set(cs),
      error: (e) => this.error.set(msgError(e)),
    });
  }

  cargarHistorial(): void {
    this.api.transacciones(15).subscribe({
      next: (ts) => this.historial.set(ts),
      error: (e) => this.error.set(msgError(e)),
    });
  }

  operar(): void {
    this.resultado.set(null);
    this.error.set(null);
    this.ocupado.set(true);

    this.api
      .operar(this.tipo(), {
        cuentaOrigen: this.cuentaOrigen,
        cuentaDestino: this.necesitaDestino() ? this.cuentaDestino : undefined,
        importe: this.necesitaImporte() ? (this.importe ?? undefined) : undefined,
        timeoutMs: this.timeoutMs,
        simularDemoraMs: this.simularDemora ? 3000 : undefined,
      })
      .subscribe({
        next: (r) => {
          this.resultado.set(r);
          this.ocupado.set(false);
          this.cargarCuentas();
          this.cargarHistorial();
        },
        error: (e) => {
          this.error.set(msgError(e));
          this.ocupado.set(false);
          this.cargarHistorial();
        },
      });
  }

  chipResultado(r: string): string {
    if (r === 'APROBADA') return 'chip verde';
    if (r === 'RECHAZADA') return 'chip rojo';
    return 'chip ambar'; // TIMEOUT
  }

  fmt = dineroCents;
  fmtMonto = dinero;
  nombrar = etiquetaTipo;
}
