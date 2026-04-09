package main.model;

public class Product {
    private final String id;
    private final String name;
    private final String type;
    private final double price;
    private final int stock;

    public Product(String id, String name, String type, double price, int stock) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.stock = stock;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
}