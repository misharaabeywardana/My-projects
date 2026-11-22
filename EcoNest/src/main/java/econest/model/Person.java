package econest.model;

/**
 * Shared contact details for any person the business deals with.
 * Client and DeliveryAgent both inherit from this class.
 */
public abstract class Person extends BaseEntity {

    private String name;
    private String phone;
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** Each kind of person describes its role differently (polymorphism). */
    public abstract String getRoleDescription();

    @Override
    public String getDisplayName() {
        return name;
    }
}
