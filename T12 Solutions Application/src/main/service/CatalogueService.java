package main.service;

import main.model.Product;

import java.util.ArrayList;
import java.util.List;

public class CatalogueService {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();

        products.add(new Product("10000001", "Paracetamol", "Caps", 0.10, 10345));
        products.add(new Product("10000002", "Aspirin", "Caps", 0.50, 12453));
        products.add(new Product("10000003", "Analgin", "Caps", 1.20, 4235));
        products.add(new Product("20000004", "Iodine tincture", "Bottle", 0.30, 22134));
        products.add(new Product("20000005", "Rhynol", "Bottle", 2.50, 1908));

        return products;
    }
}