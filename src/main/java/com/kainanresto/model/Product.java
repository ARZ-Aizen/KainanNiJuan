package com.kainanresto.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Row model for the inventory table. Values are kept as strings so the
 * server/database layer can supply already-formatted display text
 * (for example "25 kg"); swap to typed properties if you prefer to format
 * inside the cell factories instead.
 */
public class Product {

    private final StringProperty sku = new SimpleStringProperty(this, "sku", "");
    private final StringProperty itemName = new SimpleStringProperty(this, "itemName", "");
    private final StringProperty category = new SimpleStringProperty(this, "category", "");
    private final StringProperty uom = new SimpleStringProperty(this, "uom", "");
    private final StringProperty qtyOnHand = new SimpleStringProperty(this, "qtyOnHand", "");
    private final StringProperty parLevel = new SimpleStringProperty(this, "parLevel", "");
    private final StringProperty status = new SimpleStringProperty(this, "status", "");

    public Product() {
    }

    public Product(String sku, String itemName, String category, String uom,
                         String qtyOnHand, String parLevel, String status) {
        setSku(sku);
        setItemName(itemName);
        setCategory(category);
        setUom(uom);
        setQtyOnHand(qtyOnHand);
        setParLevel(parLevel);
        setStatus(status);
    }

    public StringProperty skuProperty() { return sku; }
    public String getSku() { return sku.get(); }
    public void setSku(String value) { sku.set(value); }

    public StringProperty itemNameProperty() { return itemName; }
    public String getItemName() { return itemName.get(); }
    public void setItemName(String value) { itemName.set(value); }

    public StringProperty categoryProperty() { return category; }
    public String getCategory() { return category.get(); }
    public void setCategory(String value) { category.set(value); }

    public StringProperty uomProperty() { return uom; }
    public String getUom() { return uom.get(); }
    public void setUom(String value) { uom.set(value); }

    public StringProperty qtyOnHandProperty() { return qtyOnHand; }
    public String getQtyOnHand() { return qtyOnHand.get(); }
    public void setQtyOnHand(String value) { qtyOnHand.set(value); }

    public StringProperty parLevelProperty() { return parLevel; }
    public String getParLevel() { return parLevel.get(); }
    public void setParLevel(String value) { parLevel.set(value); }

    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
}