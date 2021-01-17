package com.example.vendingservice;

import java.io.Serializable;
import java.util.ArrayList;

public class VendingDisplacement implements Serializable {
    public String name;
    public String addr;
    public ArrayList<ProductLine> lines;

    public VendingDisplacement(String name, String addr) {
        this.addr = addr;
        this.name = name;
        this.lines = new ArrayList<>();
    }
}