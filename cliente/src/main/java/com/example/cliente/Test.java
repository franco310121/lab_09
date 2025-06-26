package com.example.cliente;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Test {

    public static void main(String[] args) {
        try {
            // URL del servidor Flask en la nube (ajusta IP o dominio)
            URL url = new URL("http://161.132.45.205:5000/api/voucher");

            // Abrir conexión
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // JSON que se enviará al servidor
            String jsonInput = """
            {
              "id_tienda": 1,
              "total": 0.0,
              "nombre_cliente": "Cliente"
            }
            """;

            // Enviar solicitud
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Leer respuesta
            Scanner scanner = new Scanner(conn.getInputStream(), "utf-8");
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            System.out.println("Respuesta del servidor:");
            System.out.println(response);

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
