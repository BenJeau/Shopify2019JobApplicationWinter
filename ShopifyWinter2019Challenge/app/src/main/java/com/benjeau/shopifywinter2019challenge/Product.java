package com.benjeau.shopifywinter2019challenge;

import java.util.List;

/**
 * Helper POJO for the Products class respecting the Gson format
 */
public class Product {
    public String body_html;
    public String title;
    public String id;
    public String tags;
    public String vendor;
    public Image image;
    public List<Variants> variants;
}

class Image {
    public String src;
}

class Variants {
    public String inventory_quantity;
    public String price;
}