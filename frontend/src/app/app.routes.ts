import { Routes } from '@angular/router';
import { CuentasPage } from './pages/cuentas/cuentas';
import { OperacionesPage } from './pages/operaciones/operaciones';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'operaciones' },
  { path: 'cuentas', component: CuentasPage },
  { path: 'operaciones', component: OperacionesPage },
  { path: '**', redirectTo: 'operaciones' },
];
