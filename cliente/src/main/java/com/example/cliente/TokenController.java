package com.example.cliente;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class TokenController {

    @FXML private TextField phoneField;
    @FXML private TextField codeField;

    private boolean validado = false;

    private static final String SERVER_URL = "http://161.132.45.205:5002";

    public boolean isValidado() {
        return validado;
    }

    @FXML
    public void onSendToken() {
        String phone = phoneField.getText().trim();
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
            System.out.println("Enviando JSON a: " + url);
            System.out.println("Payload: " + jsonInput);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Código HTTP recibido: " + responseCode);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                in.close();
                System.out.println("Respuesta exitosa: " + content);
                showAlert("Token enviado al teléfono +51" + phone);

            } else {
                System.err.println("ERROR AL ENVIAR TOKEN:");
                System.err.println("Código HTTP: " + responseCode);
                System.err.println("Mensaje: " + conn.getResponseMessage());

                // Cabeceras de respuesta
                System.err.println("Cabeceras:");
                conn.getHeaderFields().forEach((k, v) -> System.err.println("  " + k + ": " + v));

                // Cuerpo del error
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                StringBuilder errorBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorBody.append(line).append("\n");
                }
                br.close();
                System.err.println("Cuerpo del error:");
                System.err.println(errorBody.toString());

                showAlert("Error del servidor:\n" + errorBody.toString().trim());
            }

        } catch (Exception e) {
            System.err.println("EXCEPCIÓN DETECTADA:");
            e.printStackTrace();
            showAlert("Error de conexión:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @FXML
    public void onValidateToken() {
        String phone = phoneField.getText().trim();
        String token = codeField.getText().trim();

        if (phone.isEmpty() || token.isEmpty()) {
            showAlert("Error: Teléfono y token requeridos");
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
            validado = jsonResponse.getBoolean("valido");
            String mensaje = jsonResponse.getString("mensaje");

            if (validado) {
                showAlert("Verificación exitosa: " + mensaje);
            } else {
                // Detecta si el token expiró
                if (mensaje.toLowerCase().contains("expirado")) {
                    showResendOption(phone);
                } else {
                    showAlert("Token inválido: " + mensaje);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error de conexión al validar token: " + e.getMessage());
        }
    }

    private void showResendOption(String phone) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Token expirado");
        alert.setHeaderText("El token ha expirado");
        alert.setContentText("¿Deseas reenviar el token al número +51" + phone + "?");

        alert.getButtonTypes().setAll(
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.YES) {
                onSendToken();
                codeField.clear();
            }
        });
    }


    @FXML
    private void onConfirm() {
        Stage stage = (Stage) phoneField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
