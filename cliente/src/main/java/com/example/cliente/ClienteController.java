package com.example.cliente;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
    private TextField customerNameField;

    @FXML
    private TextField customerPhoneField;

    @FXML
    private TextField customerCodeField;

    @FXML
    private TextField cashierField;

    @FXML
    private TextField cashboxIdField;

    private ObservableList<ProductoCarrito> carrito = FXCollections.observableArrayList();

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
        String nombre = customerNameField.getText();
        String telefono = customerPhoneField.getText();
        String code = customerCodeField.getText();
        String cajero = cashierField.getText();
        String idCaja = cashboxIdField.getText();

        if (nombre.isEmpty() || telefono.isEmpty() ) {
            showAlert("Debe completar todos los datos del cliente y de caja.");
            return;
        }

        if (carrito.isEmpty()) {
            showAlert("El carrito está vacío.");
            return;
        }

        // Aquí puedes enviar los datos a un servicio o imprimir la venta.
        showAlert("Venta iniciada exitosamente.");
        carrito.clear();
        actualizarTotal();
        customerNameField.clear();
        customerPhoneField.clear();
        customerCodeField.clear();
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
