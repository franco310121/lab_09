package com.example.cliente;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.util.Scanner;


public class ClienteController {

    @FXML
    private ComboBox<String> productComboBox;

    @FXML
    private TextField quantityField;

    @FXML
    private TableView<ProductoCarrito> cartTableView;

    @FXML
    private TableColumn<ProductoCarrito, String> nameColumn;

    @FXML
    private TableColumn<ProductoCarrito, Integer> quantityColumn;

    @FXML
    private TableColumn<ProductoCarrito, Double> subtotalColumn;

    @FXML
    private Label totalLabel;

    @FXML
    private Label voucherIdLabel;

    @FXML
    private TextField customerNameField;

    @FXML
    private VBox formContainer;

    @FXML
    private TextArea logTextArea;

    private ObservableList<ProductoCarrito> carrito = FXCollections.observableArrayList();

    private static final String SERVER_URL = "http://161.132.45.205:5000";

    ProductoDAO productoDAO = new ProductoDAO();
    List<Producto> productos = productoDAO.obtenerTodosLosProductos();

    @FXML
    public void initialize() {

        for (Producto p : productos) {
            productComboBox.getItems().add(p.getNombre());
        }

        // Configuración de columnas
        nameColumn.setCellValueFactory(data -> data.getValue().nombreProperty());
        quantityColumn.setCellValueFactory(data -> data.getValue().cantidadProperty().asObject());
        subtotalColumn.setCellValueFactory(data -> data.getValue().subtotalProperty().asObject());

        cartTableView.setItems(carrito);
    }

    @FXML
    public void onSincronizarClick() {
        sincronizarVentasLocales();
    }

    public void sincronizarVentasLocales() {
        try {
            // 1. Conexión local
            String url = "jdbc:mysql://localhost:3306/tienda.db";
            String user = "root";
            String password = "";

            Connection connLocal = DriverManager.getConnection(url, user, password);

            // 2. Buscar vouchers validados y no enviados
            PreparedStatement ps = connLocal.prepareStatement("SELECT * FROM voucher_local WHERE estado = 'validado' AND enviado = 0");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String voucherIdLocal = rs.getString("voucher_id_local");
                Date fecha = rs.getDate("fecha");
                Time hora = rs.getTime("hora");
                int idTienda = rs.getInt("id_tienda");
                double total = rs.getDouble("total");
                String nombreCliente = rs.getString("nombre_cliente");

                // 3. POST para generar nuevo voucher remoto
                String nombre = customerNameField.getText().isEmpty() ? "Cliente" : customerNameField.getText();
                String jsonPost = String.format("""
                                            {
                                              "id_tienda": 1,
                                              "total": 0.0,
                                              "nombre_cliente": "%s"
                                            }
                                            """, nombre);
                URL urlPost = new URL("http://161.132.45.205:5000/api/voucher");
                HttpURLConnection connPost = (HttpURLConnection) urlPost.openConnection();
                connPost.setRequestMethod("POST");
                connPost.setDoOutput(true);
                connPost.setRequestProperty("Content-Type", "application/json");
                connPost.getOutputStream().write(jsonPost.getBytes("utf-8"));

                Scanner scanner = new Scanner(connPost.getInputStream(), "utf-8");
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                String newVoucherId = response.replaceAll("[^0-9]", "");

                // 4. PUT para actualizar cabecera remota
                JSONObject jsonPut = new JSONObject();
                jsonPut.put("voucher_id", newVoucherId);
                jsonPut.put("id_tienda", idTienda);
                jsonPut.put("total", total);
                jsonPut.put("nombre_cliente", nombreCliente);
                jsonPut.put("estado", "validado");

                HttpURLConnection connPut = (HttpURLConnection) new URL("http://161.132.45.205:5000/api/voucher").openConnection();
                connPut.setRequestMethod("PUT");
                connPut.setDoOutput(true);
                connPut.setRequestProperty("Content-Type", "application/json");
                connPut.getOutputStream().write(jsonPut.toString().getBytes("utf-8"));
                connPut.getInputStream().close();


                // 7. Marcar como enviado
                PreparedStatement psUpdate = connLocal.prepareStatement("UPDATE voucher_local SET enviado = 1 WHERE voucher_id_local = ?");
                psUpdate.setString(1, voucherIdLocal);
                psUpdate.executeUpdate();
            }

            showAlert("Sincronización completa con el servidor.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error durante la sincronización: " + e.getMessage());
        }
    }



    @FXML
    public void onAddToCart() {
        String producto = productComboBox.getValue();
        String cantidadText = quantityField.getText();
        if (producto == null || cantidadText.isEmpty()) {
            showAlert("Debe seleccionar un producto y especificar la cantidad.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadText);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("La cantidad debe ser un número entero positivo.");
            return;
        }

        double precioUnitario = obtenerPrecio(producto);
        double subtotal = cantidad * precioUnitario;
        BigDecimal bd = new BigDecimal(subtotal);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        subtotal = bd.doubleValue();

        carrito.add(new ProductoCarrito(producto, cantidad, subtotal));
        actualizarTotal();
        quantityField.clear();
    }

    private void actualizarTotal() {
        double total = carrito.stream().mapToDouble(ProductoCarrito::getSubtotal).sum();
        totalLabel.setText(String.format("S/ %.2f", total));
    }

    @FXML
    public void onStartSale() {

        String nombre = customerNameField.getText().trim();
        System.out.println(voucherIdLabel.getText());

        if (carrito.isEmpty()) {
            showAlert("El carrito está vacío.");
            return;
        }
        if (nombre.isEmpty()){
            showAlert("El nombre del cliente esta vacio.");
            return;
        }

        double total = carrito.stream().mapToDouble(ProductoCarrito::getSubtotal).sum();
        if (total <= 0) {
            showAlert("El total de la venta debe ser mayor a cero.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("token_verificacion.fxml"));
            Parent root = loader.load();

            TokenController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Verificación de Token");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (!controller.isValidado()) {
                showAlert("Debe validar el token antes de continuar.");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("No se pudo abrir la ventana de verificación.");
            return;
        }

        try {
            // 1. Preparar JSON con org.json
            JSONObject json = new JSONObject();
            String rawVoucher = voucherIdLabel.getText().trim().replaceAll("\\s", "");
            json.put("voucher_id", rawVoucher);

            json.put("id_tienda", 1);
            json.put("total", total);
            json.put("nombre_cliente", nombre);
            json.put("estado", "validado");

            String jsonInput = json.toString();
            System.out.println("Enviando JSON: " + jsonInput);

            // 2. Enviar PUT al servidor para actualizar cabecera
            URL url = new URL(SERVER_URL + "/api/voucher");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");  // ← usamos PUT para actualizar
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), "utf-8");
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                // 3. Insertar detalle en base local
                DetalleVentaDAO dao = new DetalleVentaDAO();
                for (ProductoCarrito p : carrito) {
                    dao.insertarDetalle(Long.parseLong(voucherIdLabel.getText().trim()), p.getNombre(), p.getCantidad(), p.getSubtotal());
                }

                showAlert("Venta registrada exitosamente con voucher: " + voucherIdLabel.getText().trim());

                // 4. Limpiar campos
                carrito.clear();
                actualizarTotal();
                customerNameField.clear();
                productComboBox.getSelectionModel().clearSelection();
                quantityField.clear();
                voucherIdLabel.setText("---");

                formContainer.setVisible(false);
                formContainer.setManaged(false);

            } else {
                Scanner scanner = new Scanner(conn.getErrorStream(), "utf-8");
                String error = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "Error desconocido";
                scanner.close();
                showAlert("Error al registrar venta en servidor: " + error);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ocurrió un error al conectar con el servidor.");
            showAlert("Se procedera a guardar localmente.");
            LocalMethots.guardarDetallesLocales(voucherIdLabel.getText(), carrito);

            LocalMethots.actualizarCabeceraLocal(voucherIdLabel.getText(), nombre, total);
            // 4. Limpiar campos
            carrito.clear();
            actualizarTotal();
            customerNameField.clear();
            productComboBox.getSelectionModel().clearSelection();
            quantityField.clear();
            voucherIdLabel.setText("---");

            formContainer.setVisible(false);
            formContainer.setManaged(false);
        }
    }

    @FXML
    public void onCancel() {
        try {
            String voucher = voucherIdLabel.getText().trim();
            if (!voucher.equals("---") && !voucher.isEmpty()) {
                // Llamada PATCH para cancelar voucher
                URL url = new URL(SERVER_URL + "/api/voucher/cancelar");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                String jsonInput = String.format("{\"voucher_id\": \"%s\"}", voucher);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    Scanner scanner = new Scanner(conn.getErrorStream(), "utf-8");
                    String error = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "Error desconocido";
                    scanner.close();
                    showAlert("Error al cancelar voucher remoto");
                    showAlert("Buscando cancelar voucher local");
                    LocalMethots.cancelarVoucherLocal(voucherIdLabel.getText());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("No se pudo cancelar el voucher remoto.");

        }

        // Limpiar campos
        carrito.clear();
        actualizarTotal();
        quantityField.clear();
        customerNameField.clear();
        productComboBox.getSelectionModel().clearSelection();
        voucherIdLabel.setText("---");
        formContainer.setVisible(false);
        formContainer.setManaged(false);
    }


    @FXML
    public void onRequestVoucher() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        try {
            // URL del servidor Flask
            URL url = new URL(SERVER_URL + "/api/voucher");

            // Abrir conexión
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // Cuerpo del JSON
            String nombre = customerNameField.getText().isEmpty() ? "Cliente" : customerNameField.getText();
            String jsonInput = String.format("""
        {
          "id_tienda": 1,
          "total": 0.0,
          "nombre_cliente": "%s"
        }
        """, nombre);

            // Enviar JSON
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Leer respuesta
            Scanner scanner = new Scanner(conn.getInputStream(), "utf-8");
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            // Extraer voucher_id del JSON (simplemente si es {"voucher_id": 123})
            String id = response.replaceAll("[^0-9]", "");
            voucherIdLabel.setText(id.isEmpty() ? "---" : id);

        } catch (Exception e) {
            String mensajeAmigable = switch (e.getClass().getSimpleName()) {
                case "FileNotFoundException" -> "El recurso solicitado no fue encontrado en el servidor.";
                case "ConnectException" -> "No se pudo conectar al servidor. Verifica tu conexión.";
                case "SocketTimeoutException" -> "Tiempo de espera agotado al contactar al servidor.";
                default -> "Ocurrió un error al comunicar con el servidor.";
            };

            showAlert(mensajeAmigable);
            showAlert("Servidor no disponible. Se generará un voucher temporal local.");

            voucherIdLabel.setText(LocalMethots.generarVoucherLocal(customerNameField.getText()));

        }

    }

    private double obtenerPrecio(String producto) {
        for (Producto p : productos) {
            if (producto.equals(p.getNombre())){
                return p.getPrecio();
            }
        }
        return -1;
    }

    private void showAlert(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
