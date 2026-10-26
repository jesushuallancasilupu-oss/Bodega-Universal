'use strict';

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('[data-remove-line]').forEach(boton => boton.addEventListener('click', () => {
    const campoCantidad = boton.closest('form')?.querySelector('[name=cantidad]');
    if (campoCantidad) campoCantidad.value = '0';
  }));

  const campoHoraRecojo = document.querySelector('[name=horaRecojo]');
  if (campoHoraRecojo) {
    const fechaMinima = new Date(Date.now() + 30 * 60000);
    const completarDosDigitos = numero => String(numero).padStart(2, '0');
    campoHoraRecojo.min = `${fechaMinima.getFullYear()}-${completarDosDigitos(fechaMinima.getMonth() + 1)}-${completarDosDigitos(fechaMinima.getDate())}T${completarDosDigitos(fechaMinima.getHours())}:${completarDosDigitos(fechaMinima.getMinutes())}`;
  }
});
