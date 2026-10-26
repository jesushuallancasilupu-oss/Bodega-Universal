package com.pe.universal.manager;

import com.pe.universal.exception.NegocioException;
import com.pe.universal.model.Producto;
import java.math.BigDecimal;
import java.util.Set;


public final class Reglas {
    private Reglas() {}

    public static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new NegocioException(mensaje);
    }

    public static void cantidad(int cantidad) {
        exigir(cantidad > 0, "La cantidad debe ser positiva");
    }

    public static void cantidadPorVenta(Producto producto, int cantidad) {
        cantidad(cantidad);
        int incremento = producto.getCantidadPorVenta();
        exigir(incremento > 0, "La cantidad por venta no es válida");
        exigir(cantidad % incremento == 0,
                producto.getNombre() + " se vende en cantidades de " + incremento + " unidades");
    }

    public static BigDecimal positivo(BigDecimal monto) {
        exigir(monto != null && monto.signum() > 0, "El importe debe ser mayor que cero");
        exigir(monto.scale() <= 2, "Máximo dos decimales");
        exigir(monto.compareTo(new BigDecimal("99999999.99")) <= 0,
                "Importe supera DECIMAL(10,2)");
        return monto;
    }

    public static BigDecimal noNegativo(BigDecimal monto) {
        exigir(monto != null && monto.signum() >= 0, "Importe negativo");
        exigir(monto.scale() <= 2, "Máximo dos decimales");
        exigir(monto.compareTo(new BigDecimal("99999999.99")) <= 0,
                "Importe supera DECIMAL(10,2)");
        return monto;
    }

    public static BigDecimal importe(BigDecimal precio, int cantidad) {
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }

    public static void transicion(String actual, String siguiente) {
        boolean valido = switch (actual) {
            case "BORRADOR" -> Set.of("PENDIENTE", "CANCELADO").contains(siguiente);
            case "PENDIENTE" -> Set.of("EN_PREPARACION", "CANCELADO").contains(siguiente);
            case "EN_PREPARACION" -> Set.of("LISTO", "CANCELADO").contains(siguiente);
            case "LISTO" -> Set.of("ENTREGADO", "CANCELADO").contains(siguiente);
            default -> false;
        };
        exigir(valido, "Transición de estado no permitida: " + actual + " -> " + siguiente);
    }
}
