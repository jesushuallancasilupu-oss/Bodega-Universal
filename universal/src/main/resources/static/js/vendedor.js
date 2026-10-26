'use strict';

document.addEventListener('DOMContentLoaded', () => {
  activarMenuLateral();
  document.querySelectorAll('[data-order-payment]').forEach(formulario => {
    const campoMetodo = formulario.querySelector('[name=metodo]');
    const campoRecibido = formulario.querySelector('[name=recibido]');
    campoMetodo?.addEventListener('change', () => {
      if (campoMetodo.value === 'YAPE') campoRecibido.value = campoRecibido.defaultValue;
    });
  });
  const formularioVenta = document.querySelector('[data-pos]');
  if (formularioVenta) iniciarPuntoVenta(formularioVenta);
});

function iniciarPuntoVenta(formulario) {
  const productosSeleccionados = new Map();
  const listaCarrito = document.getElementById('pos-carrito');
  const etiquetaTotal = document.getElementById('pos-total');
  const campoEfectivo = document.getElementById('pos-recibido');
  const campoMetodo = document.getElementById('pos-metodo');
  const etiquetaVuelto = document.getElementById('pos-vuelto');
  const botonCobrar = document.getElementById('pos-submit');
  const camposOcultos = document.getElementById('pos-hidden');
  let total = 0;

  document.querySelectorAll('[data-pos-product]').forEach(boton => boton.addEventListener('click', () => {
    const { id, nombre, precio, stock: existenciasDisponibles, incremento } = boton.dataset;
    const producto = productosSeleccionados.get(id) || {
      nombre, precio: Number(precio), existenciasDisponibles: Number(existenciasDisponibles),
      incremento: Number(incremento) || 1, cantidad: 0
    };
    if (producto.cantidad + producto.incremento <= producto.existenciasDisponibles) {
      producto.cantidad += producto.incremento;
      productosSeleccionados.set(id, producto);
      dibujarCarrito();
    } else {
      alert('No puedes superar el stock disponible');
    }
  }));

  listaCarrito.addEventListener('click', evento => {
    const boton = evento.target.closest('[data-cart-action]');
    if (!boton) return;
    const identificador = boton.dataset.cartId;
    const producto = productosSeleccionados.get(identificador);
    if (!producto) return;
    const cantidadNueva = producto.cantidad + Number(boton.dataset.cartAction) * producto.incremento;
    if (cantidadNueva <= 0) productosSeleccionados.delete(identificador);
    else if (cantidadNueva <= producto.existenciasDisponibles) producto.cantidad = cantidadNueva;
    dibujarCarrito();
  });

  campoMetodo.addEventListener('change', () => {
    if (campoMetodo.value === 'YAPE') campoEfectivo.value = total.toFixed(2);
    calcularVuelto();
  });
  campoEfectivo.addEventListener('input', calcularVuelto);

  function calcularVuelto() {
    const montoRecibido = Number(campoEfectivo.value || 0);
    etiquetaVuelto.textContent = 'S/ ' + Math.max(0, montoRecibido - total).toFixed(2);
    const pagoValido = productosSeleccionados.size > 0 && montoRecibido >= total - .001 &&
      (campoMetodo.value === 'EFECTIVO' || Math.abs(montoRecibido - total) < .001);
    botonCobrar.disabled = !pagoValido;
  }

  function dibujarCarrito() {
    total = 0;
    listaCarrito.textContent = '';
    camposOcultos.textContent = '';
    for (const [identificador, producto] of productosSeleccionados) {
      const importeParcial = producto.cantidad * producto.precio;
      total += importeParcial;
      const fila = document.createElement('div');
      fila.className = 'item-line';
      const descripcion = document.createElement('div');
      descripcion.className = 'grow';
      descripcion.textContent = `${producto.cantidad} × ${producto.nombre}`;
      const subtotal = document.createElement('strong');
      subtotal.textContent = 'S/ ' + importeParcial.toFixed(2);
      const controles = document.createElement('div');
      controles.className = 'qty-control';
      for (const [variacion, simbolo] of [[-1, '−'], [1, '+']]) {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.dataset.cartId = identificador;
        boton.dataset.cartAction = variacion;
        boton.textContent = simbolo;
        controles.append(boton);
      }
      fila.append(descripcion, controles, subtotal);
      listaCarrito.append(fila);

      for (const [nombreCampo, valorCampo] of [['ids', identificador], ['cantidades', producto.cantidad]]) {
        const campoOculto = document.createElement('input');
        campoOculto.type = 'hidden';
        campoOculto.name = nombreCampo;
        campoOculto.value = valorCampo;
        camposOcultos.append(campoOculto);
      }
    }
    if (!productosSeleccionados.size) {
      listaCarrito.innerHTML = '<p class="muted tiny">Agrega productos para comenzar.</p>';
    }
    etiquetaTotal.textContent = 'S/ ' + total.toFixed(2);
    if (campoMetodo.value === 'YAPE') campoEfectivo.value = total.toFixed(2);
    calcularVuelto();
  }

  dibujarCarrito();
  formulario.addEventListener('submit', evento => {
    calcularVuelto();
    if (botonCobrar.disabled) {
      evento.preventDefault();
      return;
    }
    botonCobrar.disabled = true;
    botonCobrar.textContent = 'Procesando cobro…';
  });
}
