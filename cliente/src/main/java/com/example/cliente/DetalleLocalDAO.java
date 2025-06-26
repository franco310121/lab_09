package com.example.cliente;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DetalleLocalDAO {

    private final String url = "jdbc:mysql://localhost:3306/tienda";
    private final String user = "root";
    private final String password = "";

    public void insertarDetalle(String voucherIdLocal, String producto, int cantidad, double subtotal) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO detalle_local (voucher_id_local, producto, cantidad, subtotal) VALUES (?, ?, ?, ?)"
            );
            ps.setString(1, voucherIdLocal);
            ps.setString(2, producto);
            ps.setInt(3, cantidad);
            ps.setDouble(4, subtotal);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}