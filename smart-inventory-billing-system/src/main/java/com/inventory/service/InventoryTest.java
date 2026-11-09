
package com.inventory.service;

import com.inventory.model.Product;

public class InventoryTest {

    public static void main(String[] args) {

        InventoryService service = new InventoryService();

        try {

            // Add Product
            Product p = service.searchProduct("P104");

            // View Products
            System.out.println("\nAll Products:");

            for (Product product : service.getAllProducts()) {
                System.out.println(
                    product.getProductId() + " | " +
                    product.getName() + " | " +
                    product.getPrice() + " | " +
                    product.getQuantity()
                );
            }

            // Search Product
            Product found = service.searchProduct("P104");

            if (found != null) {
                System.out.println("\nProduct Found: " + found.getName());
            }

// Search Product by Name
System.out.println("\nSearching by Name:");

for (Product product : service.searchProductsByName("Soap")) {
    System.out.println(
        product.getProductId() + " | " +
        product.getName() + " | " +
        product.getPrice()
    );
}

// Update Product
p.setPrice(130.00);
p.setQuantity(30);

service.updateProduct(p);
System.out.println("Product updated successfully!");

service.deleteProduct("P104");
System.out.println("Product deleted successfully!");



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
