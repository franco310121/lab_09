package com.example.cliente;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class TwilioClient extends JFrame {
    private JTextField phoneField;
    private JTextField tokenField;
    private JButton sendButton;
    private JButton validateButton;
    private JTextArea resultArea;
    private static final String SERVER_URL = "http://161.132.45.205:5002"; // Cambia a tu IP

    public TwilioClient() {
        setTitle("Validador de Tokens vía SMS");
        setSize(500, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel superior
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        phoneField = new JTextField();
        tokenField = new JTextField();
        inputPanel.add(new JLabel("Teléfono (sin +51):"));
        inputPanel.add(phoneField);
        inputPanel.add(new JLabel("Token:"));
        inputPanel.add(tokenField);

        // Panel central
        JPanel buttonPanel = new JPanel();
        sendButton = new JButton("Enviar Token");
        validateButton = new JButton("Validar Token");
        buttonPanel.add(sendButton);
        buttonPanel.add(validateButton);

        // Panel inferior
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Eventos
        sendButton.addActionListener(e -> sendToken());
        validateButton.addActionListener(e -> validateToken());

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
    }

    private void sendToken() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            resultArea.setText("Error: Ingresa un número de teléfono (sin +51)");
            return;
        }

        try {
            URL url = new URL(SERVER_URL + "/enviar_token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = "{\"telefono\": \"" + phone + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            if (conn.getResponseCode() == 200) {
                resultArea.setText("✓ Token enviado al teléfono: " + phone);
            } else {
                resultArea.setText("✗ Error. Código HTTP: " + conn.getResponseCode());
            }
        } catch (Exception ex) {
            resultArea.setText("✗ Error de conexión: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void validateToken() {
        String phone = phoneField.getText().trim();
        String token = tokenField.getText().trim();
        if (phone.isEmpty() || token.isEmpty()) {
            resultArea.setText("✗ Error: Teléfono y Token requeridos");
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
            boolean isValid = jsonResponse.getBoolean("valido");
            String message = jsonResponse.getString("mensaje");
            resultArea.setText("🔍 Resultado:\n" + message + "\nVálido: " + isValid);
        } catch (Exception ex) {
            resultArea.setText("✗ Error de conexión: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TwilioClient client = new TwilioClient();
            client.setVisible(true);
        });
    }
}