package id.co.veritrans.sdk.coreflow.models;

/**
 * It hold an information about purchased item, </p>like
 * id, price etc.
 *
 * Created by shivam on 10/29/15.
 */
public class ItemDetails {

    private String id;
    private double price;
    private double quantity;
    private String name;

    public ItemDetails() {
    }

    /**
     *
     * @param id unique id of the item.
     * @param price price of the item.
     * @param quantity number of items that is purchased.
     * @param name name of the item.
     */
    public ItemDetails(String id, double price, double quantity, String name) {
        this.id = id;
        this.price = price;
        this.quantity = quantity;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}