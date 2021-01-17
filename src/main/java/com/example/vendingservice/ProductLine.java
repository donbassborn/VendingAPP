package com.example.vendingservice;

import java.io.Serializable;
import java.util.ArrayList;

public class ProductLine implements Serializable {
    public int height;
    public int width;
    public ArrayList<Product> products;

    public ProductLine(int height, int width) {
        this.height = height;
        this.width = width;
        this.products = new ArrayList<>();
    }
}