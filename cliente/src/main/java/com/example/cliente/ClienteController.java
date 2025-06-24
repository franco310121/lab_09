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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private TextField customerPhoneField;

    @FXML
    private TextField customerCodeField;

    @FXML
    private TextField cashierField;

    @FXML
    private TextField cashboxIdField;

    @FXML
    private VBox formContainer;

    private ObservableList<ProductoCarrito> carrito = FXCollections.observableArrayList();

    private static final String SERVER_URL = "http://161.132.45.205:5002";

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
    public void onAddToCart() {
        String producto = productComboBox.getValue();
        String cantidadText = quantityField.getText();
        String voucherId = voucherIdLabel.getText();
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
        /*
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
        }*/

        String nombre = customerNameField.getText().trim();
        String telefono = customerPhoneField.getText().trim();

        if (nombre.isEmpty() || telefono.isEmpty()) {
            showAlert("Debe completar el nombre y teléfono del cliente.");
            return;
        }

        if (carrito.isEmpty()) {
            showAlert("El carrito está vacío.");
            return;
        }

        double total = carrito.stream().mapToDouble(ProductoCarrito::getSubtotal).sum();
        if (total <= 0) {
            showAlert("El total de la venta debe ser mayor a cero.");
            return;
        }

        try {
            // 1. Preparar JSON con org.json
            JSONObject json = new JSONObject();
            String rawVoucher = voucherIdLabel.getText().trim().replaceAll("\\s", "");
            if (!rawVoucher.startsWith("VCH-")) {
                rawVoucher = "VCH-" + rawVoucher.substring(0, 8) + "-" + rawVoucher.substring(8);
            }
            json.put("voucher_id", rawVoucher);

            json.put("id_tienda", 1);
            json.put("total", total);
            json.put("nombre_cliente", nombre);
            json.put("estado", "validado");
            System.out.println(voucherIdLabel.getText().trim());
            System.out.println(total + nombre);

            String jsonInput = json.toString();
            System.out.println("Enviando JSON: " + jsonInput);

            // 2. Enviar PUT al servidor para actualizar cabecera
            URL url = new URL("http://161.132.45.205:5000/api/voucher");
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
                customerPhoneField.clear();
                customerCodeField.clear();
                productComboBox.getSelectionModel().clearSelection();
                quantityField.clear();
                voucherIdLabel.setText("---");

            } else {
                Scanner scanner = new Scanner(conn.getErrorStream(), "utf-8");
                String error = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "Error desconocido";
                scanner.close();
                showAlert("Error al registrar venta en servidor: " + error);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ocurrió un error al conectar con el servidor.");
        }
    }





    @FXML
    public void onCancel() {
        carrito.clear();
        actualizarTotal();
        quantityField.clear();
        customerNameField.clear();
        customerPhoneField.clear();
        cashierField.clear();
        cashboxIdField.clear();
        productComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    public void onRequestVoucher() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        try {
            // URL del servidor Flask
            URL url = new URL("http://161.132.45.205:5000/api/voucher");

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
            e.printStackTrace();
            showAlert("No se pudo obtener un voucher del servidor.");
        }

    }

    @FXML
    public void onSendToken() {
        String phone = customerPhoneField.getText().trim();
        if (phone.isEmpty()) {
            showAlert("Error: Ingresa un número de teléfono (sin +51)");
            return;
        }

        try {
            URL url = new URL(SERVER_URL + "/enviar_token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = "{\"telefono\": \"" + phone + "\"}";
            System.out.println("➡ Enviando JSON a: " + url);
            System.out.println("➡ Payload: " + jsonInput);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            int responseCode = conn.getResponseCode();
            System.out.println("⬅ Código HTTP recibido: " + responseCode);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                in.close();
                System.out.println("⬅ Respuesta exitosa: " + content);
                showAlert("✓ Token enviado al teléfono +51" + phone);

            } else {
                System.err.println("✗ ERROR AL ENVIAR TOKEN:");
                System.err.println("✗ Código HTTP: " + responseCode);
                System.err.println("✗ Mensaje: " + conn.getResponseMessage());

                // Cabeceras de respuesta
                System.err.println("✗ Cabeceras:");
                conn.getHeaderFields().forEach((k, v) -> System.err.println("  " + k + ": " + v));

                // Cuerpo del error
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                StringBuilder errorBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorBody.append(line).append("\n");
                }
                br.close();
                System.err.println("✗ Cuerpo del error:");
                System.err.println(errorBody.toString());

                showAlert("✗ Error del servidor:\n" + errorBody.toString().trim());
            }

        } catch (Exception e) {
            System.err.println("✗ EXCEPCIÓN DETECTADA:");
            e.printStackTrace();
            showAlert("✗ Error de conexión:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }



    @FXML
    public void onValidateToken() {
        String phone = customerPhoneField.getText().trim();
        String token = customerCodeField.getText().trim();

        if (phone.isEmpty() || token.isEmpty()) {
            showAlert("✗ Error: Teléfono y token requeridos");
            return;
        }

        try {
            URL url = new URL(SERVER_URL + "/validar_token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = "{\"telefono\": \"" + phone + "\", \"token\": \"" + token + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            boolean valido = jsonResponse.getBoolean("valido");
            String mensaje = jsonResponse.getString("mensaje");

            if (valido) {
                showAlert("✅ Verificación exitosa: " + mensaje);
            } else {
                showAlert("❌ Token inválido: " + mensaje);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("✗ Error de conexión al validar token: " + e.getMessage());
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
