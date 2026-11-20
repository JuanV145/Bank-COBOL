import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BancoApi, msgError } from '../../core/api';
import { Cuenta } from '../../core/modelos';
import { dineroCents } from '../../core/formato';

@Component({
  selector: 'app-cuentas-page',
  imports: [FormsModule],
  templateUrl: './cuentas.html',
  styleUrl: './cuentas.css',
})
export class CuentasPage implements OnInit {
  private api = inject(BancoApi);

  cuentas = signal<Cuenta[]>([]);
  mensaje = signal<string | null>(null);
  error = signal<string | null>(null);
  ocupado = signal(false);

  numero = '';
  titular = '';
  tipo = 'A';

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.cuentas().subscribe({
      next: (cs) => this.cuentas.set(cs),
      error: (e) => this.error.set(msgError(e)),
    });
  }

  crear(): void {
    if (!this.numero || !this.titular) {
      this.error.set('Número y titular son obligatorios.');
      return;
    }
    this.error.set(null);
    this.ocupado.set(true);
    this.api
      .crearCuenta({ numero: this.numero.trim(), titular: this.titular.trim(), tipo: this.tipo })
      .subscribe({
        next: () => {
          this.mensaje.set(`Cuenta ${this.numero} creada con saldo $0.`);
          this.numero = '';
          this.titular = '';
          this.ocupado.set(false);
          this.cargar();
        },
        error: (e) => {
          this.error.set(msgError(e));
          this.ocupado.set(false);
        },
      });
  }

  fmt = dineroCents;
}
