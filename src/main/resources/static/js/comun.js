'use strict';
// Comportamientos generales que se reutilizan en varios modulos.
document.addEventListener('DOMContentLoaded', () => {
  const botonMenu = document.querySelector('[data-menu-toggle]');
  botonMenu?.addEventListener('click', () => document.querySelector('.nav-links')?.classList.toggle('open'));

  document.querySelectorAll('[data-confirm]').forEach(formulario => formulario.addEventListener('submit', evento => {
    if (!window.confirm(formulario.dataset.confirm || '¿Confirmas esta acción?')) evento.preventDefault();
  }));
  document.querySelectorAll('[data-modal-open]').forEach(boton => boton.addEventListener('click', () => {
    document.getElementById(boton.dataset.modalOpen)?.classList.add('is-open');
  }));
  document.querySelectorAll('[data-modal-close]').forEach(boton => boton.addEventListener('click', () => {
    boton.closest('.modal')?.classList.remove('is-open');
  }));
  document.querySelectorAll('.modal').forEach(ventana => ventana.addEventListener('click', evento => {
    if (evento.target === ventana) ventana.classList.remove('is-open');
  }));
  document.querySelectorAll('[data-filter]').forEach(campoBusqueda => campoBusqueda.addEventListener('input', () => {
    const busqueda = campoBusqueda.value.toLowerCase().trim();
    const contenedor = document.querySelector(campoBusqueda.dataset.filter);
    contenedor?.querySelectorAll('[data-searchable]').forEach(fila => fila.classList.toggle('hide', !fila.textContent.toLowerCase().includes(busqueda)));
  }));
  document.querySelectorAll('[data-qty]').forEach(contenedor => {
    const campoCantidad = contenedor.querySelector('input');
    contenedor.querySelectorAll('[data-qty-change]').forEach(boton => boton.addEventListener('click', () => {
      const cantidadNueva = Number(campoCantidad.value || 0) + Number(boton.dataset.qtyChange);
      campoCantidad.value = Math.max(Number(campoCantidad.min || 0), Math.min(Number(campoCantidad.max || 9999), cantidadNueva));
      campoCantidad.dispatchEvent(new Event('change', { bubbles: true }));
    }));
  });
});

function activarMenuLateral() {
  const rutaActual = window.location.pathname;
  for (const enlace of document.querySelectorAll('.sidebar .side-menu a')) {
    const rutaDestino = new URL(enlace.href).pathname;
    if (rutaActual === rutaDestino || (rutaDestino !== '/admin' && rutaDestino !== '/vendedor' && rutaActual.startsWith(rutaDestino + '/'))) {
      enlace.classList.add('active');
    }
  }
}
