package main.service;

import main.model.Product;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CACatalogueService {

    private static final String URL = "jdbc:postgresql://interchange.proxy.rlwy.net:32051/railway";
    private static final String USER = "postgres";
    private static final String PASSWORD = "masterkey";

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();

        String sql = """
            SELECT item_id, description, package_type, unit, units_in_pack,
                   package_cost, availability, min_stock_level
            FROM catalogue_items
            ORDER BY item_id
        """;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getString("item_id"),
                        rs.getString("description"),
                        rs.getString("description"),
                        rs.getString("package_type"),
                        rs.getString("unit"),
                        rs.getInt("units_in_pack"),
                        rs.getDouble("package_cost"),
                        rs.getDouble("package_cost"),
                        rs.getInt("availability"),
                        rs.getInt("min_stock_level")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }
}