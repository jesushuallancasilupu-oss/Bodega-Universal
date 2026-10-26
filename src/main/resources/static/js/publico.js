'use strict';

document.addEventListener('DOMContentLoaded', () => {
  inicializarCarritoCliente();
});

async function inicializarCarritoCliente() {
  const contadores = document.querySelectorAll('[data-cart-count]');
  const formularios = document.querySelectorAll('form[data-add-to-cart]');

  if (contadores.length > 0) {
    try {
      const respuesta = await fetch('/cliente/carrito/contador', {
        headers: { 'Accept': 'application/json' },
        credentials: 'same-origin'
      });
      if (respuesta.ok && respuesta.headers.get('content-type')?.includes('application/json')) {
        const datos = await respuesta.json();
        actualizarContadorCarrito(datos.cantidad);
      }
    } catch (error) {
      contadores.forEach(contador => { contador.textContent = '–'; });
    }
  }

  formularios.forEach(formulario => formulario.addEventListener('submit', async evento => {
    evento.preventDefault();
    const boton = formulario.querySelector('[type=submit]');
    if (formulario.dataset.enviando === '1') return;
    formulario.dataset.enviando = '1';
    if (boton) boton.disabled = true;

    try {
      const respuesta = await fetch(formulario.action, {
        method: 'POST',
        body: new URLSearchParams(new FormData(formulario)),
        credentials: 'same-origin',
        headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
      });
      if (respuesta.redirected) {

        window.location.assign(respuesta.url);
        return;
      }
      if (!respuesta.headers.get('content-type')?.includes('application/json')) {
        throw new Error('No se pudo agregar el producto. Recarga la página e inténtalo otra vez.');
      }
      const resultado = await respuesta.json();
      if (!respuesta.ok || !resultado.ok) {
        throw new Error(resultado.mensaje || 'No se pudo agregar el producto.');
      }
      actualizarContadorCarrito(resultado.cantidad);
      mostrarAvisoCarrito(resultado.mensaje || 'Producto agregado al carrito');
    } catch (error) {
      mostrarAvisoCarrito(error.message || 'No se pudo agregar el producto.', true);
    } finally {
      delete formulario.dataset.enviando;
      if (boton) boton.disabled = false;
    }
  }));
}

function actualizarContadorCarrito(cantidad) {
  const numero = Number(cantidad);
  if (!Number.isFinite(numero) || numero < 0) return;
  document.querySelectorAll('[data-cart-count]').forEach(contador => {
    contador.textContent = String(numero);
    contador.classList.remove('cart-bump');
    void contador.offsetWidth;
    contador.classList.add('cart-bump');
  });
}

function mostrarAvisoCarrito(mensaje, error = false) {
  let aviso = document.getElementById('cart-toast');
  if (!aviso) {
    aviso = document.createElement('div');
    aviso.id = 'cart-toast';
    aviso.className = 'cart-toast';
    aviso.setAttribute('role', 'status');
    aviso.setAttribute('aria-live', 'polite');
    document.body.appendChild(aviso);
  }
  aviso.classList.toggle('is-error', error);
  aviso.textContent = mensaje;
  aviso.classList.add('visible');
  if (aviso.temporizadorOcultacion) clearTimeout(aviso.temporizadorOcultacion);
  aviso.temporizadorOcultacion = setTimeout(() => aviso.classList.remove('visible'), 2800);
}
