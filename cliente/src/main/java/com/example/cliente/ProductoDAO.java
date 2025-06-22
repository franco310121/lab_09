package com.example.cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    private final String url = "jdbc:mysql://localhost:3306/tienda.db";
    private final String user = "root";
    private final String password = ""; // por defecto en XAMPP

    public List<Producto> obtenerTodosLosProductos() {
        List<Producto> productos = new ArrayList<>();

        String sql = "SELECT * FROM productos";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");
                int stock = rs.getInt("stock");

                Producto producto = new Producto(id, nombre, precio, stock);
                productos.add(producto);
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener productos: " + e.getMessage());
        }

        return productos;
    }
}
