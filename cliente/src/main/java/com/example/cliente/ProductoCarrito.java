package com.example.cliente;

import javafx.beans.property.*;

public class ProductoCarrito {
    private final StringProperty nombre;
    private final IntegerProperty cantidad;
    private final DoubleProperty subtotal;

    public ProductoCarrito(String nombre, int cantidad, double subtotal) {
        this.nombre = new SimpleStringProperty(nombre);
        this.cantidad = new SimpleIntegerProperty(cantidad);
        this.subtotal = new SimpleDoubleProperty(subtotal);
    }

    public String getNombre() {
        return nombre.get();
    }

    public int getCantidad() {
        return cantidad.get();
    }

    public double getSubtotal() {
        return subtotal.get();
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public IntegerProperty cantidadProperty() {
        return cantidad;
    }

    public DoubleProperty subtotalProperty() {
        return subtotal;
    }
}
