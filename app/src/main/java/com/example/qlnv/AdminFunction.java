package com.example.qlnv; // Hoặc package của bạn

public class AdminFunction {
    private String name;
    private int id; // Một ID để xác định chức năng khi click

    public AdminFunction(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}