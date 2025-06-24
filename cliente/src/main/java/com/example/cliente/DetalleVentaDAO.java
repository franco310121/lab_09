package com.example.cliente;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DetalleVentaDAO {

    private final String url = "jdbc:mysql://localhost:3306/tienda.db"; // OJO: sin .db
    private final String user = "root";
    private final String password = ""; // o tu contraseña si cambiaste

    public void insertarDetalle(long voucherId, String producto, int cantidad, double subtotal) {
        String sql = "INSERT INTO detalle_venta(voucher_id, producto, cantidad, subtotal) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, voucherId);
            pstmt.setString(2, producto);
            pstmt.setInt(3, cantidad);
            pstmt.setDouble(4, subtotal);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
