package com.example.cliente;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class LocalMethots {
    // NUEVOS MÉTODOS PARA SOPORTE LOCAL

    public static String generarVoucherLocal(String nombreCliente) {
        try {
            String url = "jdbc:mysql://localhost:3306/tienda.db";
            String user = "root";
            String password = "";

            Connection conn = DriverManager.getConnection(url, user, password);

            String fechaActual = LocalDate.now().toString();
            PreparedStatement psCount = conn.prepareStatement(
                    "SELECT COUNT(*) AS total FROM voucher_local WHERE fecha = ?"
            );
            psCount.setString(1, fechaActual);
            ResultSet rsCount = psCount.executeQuery();
            int correlativo = 1;
            if (rsCount.next()) {
                correlativo += rsCount.getInt("total");
            }

            String fechaCompacta = fechaActual.replace("-", "");
            String correlativoStr = String.format("%04d", correlativo);
            String voucherId = "1" + fechaCompacta + correlativoStr;

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO voucher_local (voucher_id_local, fecha, hora, id_tienda, total, nombre_cliente, estado, enviado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, voucherId);
            ps.setDate(2, java.sql.Date.valueOf(fechaActual));
            ps.setTime(3, java.sql.Time.valueOf(LocalTime.now()));
            ps.setInt(4, 1);
            ps.setDouble(5, 0.0);
            ps.setString(6, nombreCliente);
            ps.setString(7, "pendiente");
            ps.setBoolean(8, false);
            ps.executeUpdate();

            return voucherId;

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error al generar voucher local: " + ex.getMessage());
            return null;
        }
    }

    public static void actualizarCabeceraLocal(String voucherId, String nombreCliente, double total) {
        try {
            String url = "jdbc:mysql://localhost:3306/tienda.db";
            String user = "root";
            String password = "";

            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE voucher_local SET total = ?, nombre_cliente = ?, estado = ?, enviado = ? WHERE voucher_id_local = ?"
            );
            ps.setDouble(1, total);
            ps.setString(2, nombreCliente);
            ps.setString(3, "validado");
            ps.setBoolean(4, false);
            ps.setString(5, voucherId);
            ps.executeUpdate();
            showAlert("Venta guardada localmente con exito");

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error al actualizar cabecera local: " + ex.getMessage());
        }
    }

    public static void guardarDetallesLocales(String voucherId, ObservableList<ProductoCarrito> carrito) {
        try {
            String url = "jdbc:mysql://localhost:3306/tienda.db";
            String user = "root";
            String password = "";

            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO detalle_venta (voucher_id, producto, cantidad, subtotal) VALUES (?, ?, ?, ?)"
            );

            for (ProductoCarrito p : carrito) {
                ps.setString(1, voucherId);
                ps.setString(2, p.getNombre());
                ps.setInt(3, p.getCantidad());
                ps.setDouble(4, p.getSubtotal());
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error al guardar detalles localmente: " + ex.getMessage());
        }
    }

    public static void cancelarVoucherLocal(String voucherId) {
        try {
            String url = "jdbc:mysql://localhost:3306/tienda.db";
            String user = "root";
            String password = "";

            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE voucher_local SET estado = 'cancelado' WHERE voucher_id_local = ?"
            );
            ps.setString(1, voucherId);
            ps.executeUpdate();
            showAlert("Voucher local cancelado.");

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error al cancelar voucher local: " + ex.getMessage());
        }
    }

    public static void showAlert(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
