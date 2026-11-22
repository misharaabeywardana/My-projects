package econest.model;

import java.time.LocalDateTime;

/** A household or office customer of EcoNest (Task 2). */
public class Client extends Person {

    private ClientType clientType = ClientType.HOUSEHOLD;
    private String companyName;
    private String address;
    private LocalDateTime createdAt;

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String getRoleDescription() {
        return clientType == ClientType.OFFICE ? "Office client" : "Household client";
    }

    @Override
    public String getDisplayName() {
        if (clientType == ClientType.OFFICE && companyName != null && !companyName.isBlank()) {
            return getName() + " (" + companyName + ")";
        }
        return getName();
    }
}
