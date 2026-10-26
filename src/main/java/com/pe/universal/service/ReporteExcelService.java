package com.pe.universal.service;

import com.pe.universal.datos.Resumenes.LineaVista;
import com.pe.universal.datos.Resumenes.ProductoVista;
import com.pe.universal.datos.Resumenes.ReporteInventarioVista;
import com.pe.universal.datos.Resumenes.ReporteVentasVista;
import com.pe.universal.datos.Resumenes.VentaVista;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ReporteExcelService {

    public byte[] exportarVentas(ReporteVentasVista reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle encabezado = estiloEncabezado(libro);
            CellStyle moneda = estiloMoneda(libro);
            CellStyle fecha = estiloFecha(libro);
            CellStyle titulo = estiloTitulo(libro);

            Sheet resumen = libro.createSheet("Resumen");
            prepararHoja(resumen, "REPORTE DE VENTAS", "Solo ventas pagadas", 4, titulo);
            dato(resumen, 4, "Desde", reporte.desde().toString());
            dato(resumen, 5, "Hasta", reporte.hasta().toString());
            dato(resumen, 6, "Transacciones pagadas", reporte.cantidad());
            datoMoneda(resumen, 7, "Total de ventas", reporte.total().doubleValue(), moneda);
            dato(resumen, 8, "Generado", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            anchos(resumen, 28, 24, 18, 18);

            Sheet ventas = libro.createSheet("Ventas");
            prepararHoja(ventas, "VENTAS PAGADAS", "Período: " + reporte.desde() + " al " + reporte.hasta(), 8, titulo);
            encabezados(ventas, encabezado, "Venta", "Fecha", "Tipo", "ID vendedor", "ID pedido", "Total (S/)", "Pago", "Cierre de caja");
            int numeroFila = 5;
            for (VentaVista venta : reporte.ventas()) {
                Row fila = ventas.createRow(numeroFila++);
                numero(fila, 0, venta.id());
                fecha(fila, 1, venta.fecha(), fecha);
                texto(fila, 2, venta.tipo());
                numero(fila, 3, venta.vendedorId());
                numero(fila, 4, venta.pedidoId());
                moneda(fila, 5, venta.total().doubleValue(), moneda);
                texto(fila, 6, venta.pagada() ? "Pagada" : "Pendiente");
                texto(fila, 7, venta.cerrada() ? "Cerrada" : "Sin cierre");
            }
            finalizarTabla(ventas, numeroFila, 16, 23, 20, 19, 17, 19, 16, 19);

            Sheet detalles = libro.createSheet("Detalle de productos");
            prepararHoja(detalles, "PRODUCTOS VENDIDOS", "Detalle de las ventas pagadas del período", 7, titulo);
            encabezados(detalles, encabezado, "Venta", "Producto", "ID producto", "Cantidad", "Precio unitario (S/)", "Subtotal (S/)", "Fecha de venta");
            numeroFila = 5;
            for (VentaVista venta : reporte.ventas()) {
                for (LineaVista linea : venta.lineas()) {
                    Row fila = detalles.createRow(numeroFila++);
                    numero(fila, 0, venta.id());
                    texto(fila, 1, linea.producto());
                    numero(fila, 2, linea.productoId());
                    numero(fila, 3, linea.cantidad());
                    moneda(fila, 4, linea.precioUnitario().doubleValue(), moneda);
                    moneda(fila, 5, linea.subtotal().doubleValue(), moneda);
                    fecha(fila, 6, venta.fecha(), fecha);
                }
            }
            finalizarTabla(detalles, numeroFila, 14, 37, 17, 15, 24, 20, 23);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo generar el reporte de ventas en Excel.", excepcion);
        }
    }

    public byte[] exportarInventario(ReporteInventarioVista reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle encabezado = estiloEncabezado(libro);
            CellStyle moneda = estiloMoneda(libro);
            CellStyle titulo = estiloTitulo(libro);

            Sheet resumen = libro.createSheet("Resumen");
            prepararHoja(resumen, "REPORTE DE INVENTARIO", "Existencias registradas en el sistema", 4, titulo);
            dato(resumen, 4, "Productos activos", reporte.productos());
            dato(resumen, 5, "Productos bajo mínimo", reporte.bajoMinimo());
            dato(resumen, 6, "Total de registros", reporte.inventario().size());
            dato(resumen, 7, "Generado", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            anchos(resumen, 28, 24, 18, 18);

            Sheet productos = libro.createSheet("Productos");
            prepararHoja(productos, "INVENTARIO DE PRODUCTOS", "Incluye productos activos y retirados", 10, titulo);
            encabezados(productos, encabezado, "ID", "Producto", "Categoría", "Precio unitario (S/)", "Stock actual", "Stock mínimo", "Cantidad por venta", "Precio por venta (S/)", "Estado", "Nivel de stock");
            List<ProductoVista> inventarioOrdenado = reporte.inventario().stream()
                    .sorted(Comparator.comparing(ProductoVista::categoria, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(ProductoVista::nombre, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            int numeroFila = 5;
            for (ProductoVista producto : inventarioOrdenado) {
                Row fila = productos.createRow(numeroFila++);
                numero(fila, 0, producto.id());
                texto(fila, 1, producto.nombre());
                texto(fila, 2, producto.categoria());
                moneda(fila, 3, producto.precio().doubleValue(), moneda);
                numero(fila, 4, producto.stock());
                numero(fila, 5, producto.stockMinimo());
                numero(fila, 6, producto.cantidadPorVenta());
                moneda(fila, 7, producto.precio().multiply(java.math.BigDecimal.valueOf(producto.cantidadPorVenta())).doubleValue(), moneda);
                texto(fila, 8, producto.estado() == 1 ? "Activo" : "Retirado");
                texto(fila, 9, producto.estado() != 1 ? "No aplica" :
                        producto.stock() <= producto.stockMinimo() ? "Bajo mínimo" : "Normal");
            }
            finalizarTabla(productos, numeroFila, 10, 37, 24, 25, 17, 17, 23, 25, 16, 20);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo generar el reporte de inventario en Excel.", excepcion);
        }
    }

    private void prepararHoja(Sheet hoja, String nombre, String descripcion, int columnas, CellStyle estilo) {
        Row filaTitulo = hoja.createRow(0);
        Cell celda = filaTitulo.createCell(0);
        celda.setCellValue(nombre);
        celda.setCellStyle(estilo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, columnas - 1));
        filaTitulo.setHeightInPoints(30);
        hoja.createRow(1).createCell(0).setCellValue(descripcion);
    }

    private void dato(Sheet hoja, int numeroFila, String etiqueta, String valor) {
        Row fila = hoja.createRow(numeroFila);
        texto(fila, 0, etiqueta);
        texto(fila, 1, valor);
    }

    private void dato(Sheet hoja, int numeroFila, String etiqueta, long valor) {
        Row fila = hoja.createRow(numeroFila);
        texto(fila, 0, etiqueta);
        numero(fila, 1, valor);
    }

    private void datoMoneda(Sheet hoja, int numeroFila, String etiqueta, double valor, CellStyle estilo) {
        Row fila = hoja.createRow(numeroFila);
        texto(fila, 0, etiqueta);
        moneda(fila, 1, valor, estilo);
    }

    private void encabezados(Sheet hoja, CellStyle estilo, String... nombres) {
        Row fila = hoja.createRow(4);
        fila.setHeightInPoints(24);
        for (int columna = 0; columna < nombres.length; columna++) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(nombres[columna]);
            celda.setCellStyle(estilo);
        }
    }

    private void texto(Row fila, int columna, String valor) {
        fila.createCell(columna).setCellValue(valor == null ? "" : valor);
    }

    private void numero(Row fila, int columna, Number valor) {
        if (valor != null) fila.createCell(columna).setCellValue(valor.doubleValue());
    }

    private void moneda(Row fila, int columna, double valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private void fecha(Row fila, int columna, LocalDateTime valor, CellStyle estilo) {
        if (valor == null) return;
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private void finalizarTabla(Sheet hoja, int siguienteFila, int... anchos) {
        anchos(hoja, anchos);
        hoja.createFreezePane(0, 5);
        hoja.setAutoFilter(new CellRangeAddress(4, Math.max(4, siguienteFila - 1), 0, anchos.length - 1));
    }

    private void anchos(Sheet hoja, int... anchos) {
        for (int columna = 0; columna < anchos.length; columna++) {
            hoja.setColumnWidth(columna, anchos[columna] * 256);
        }
    }

    private CellStyle estiloTitulo(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 15);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle estiloEncabezado(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.BROWN.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }

    private CellStyle estiloMoneda(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("\"S/ \"#,##0.00"));
        return estilo;
    }

    private CellStyle estiloFecha(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        return estilo;
    }
}
