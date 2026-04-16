package com.repregaher.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventario")
@CrossOrigin("*")
public class InventarioUploadController {

    // ===== MODELO SIMPLE =====
    static class Producto {
        public String nombre;
        public int cantidad;
        public String categoria;

        public Producto(String nombre, int cantidad, String categoria) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.categoria = categoria;
        }
    }

    // ===== "BASE DE DATOS" EN MEMORIA =====
    private final List<Producto> inventario = new ArrayList<>();

    // ===== CARGAR EXCEL =====
    @PostMapping("/cargar")
    public String cargarExcel(@RequestParam("archivo") MultipartFile archivo) {

        if (archivo.isEmpty()) {
            return "El archivo está vacío";
        }

        inventario.clear();

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nombre = getCellString(row.getCell(0));
                Integer cantidad = getCellInt(row.getCell(1));
                String categoria = getCellString(row.getCell(2));

                // Validación básica
                if (nombre == null || cantidad == null) continue;

                inventario.add(new Producto(nombre, cantidad, categoria));
            }

            return "Inventario cargado correctamente. Total: " + inventario.size();

        } catch (Exception e) {
            return "Error al procesar archivo: " + e.getMessage();
        }
    }

    // ===== OBTENER INVENTARIO =====
    @GetMapping
    public List<Producto> obtenerInventario(@RequestParam(required = false) String buscar) {

        if (buscar != null && !buscar.isBlank()) {
            return inventario.stream()
                    .filter(p -> p.nombre != null &&
                            p.nombre.toLowerCase().contains(buscar.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return inventario;
    }

    // ===== UTIL: LEER STRING SEGURAMENTE =====
    private String getCellString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    // ===== UTIL: LEER ENTERO SEGURAMENTE =====
    private Integer getCellInt(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Integer.parseInt(cell.getStringCellValue());
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}