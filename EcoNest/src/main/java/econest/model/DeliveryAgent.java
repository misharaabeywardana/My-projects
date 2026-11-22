package econest.model;

/** A delivery person and the vehicle they use (Task 4). */
public class DeliveryAgent extends Person {

    private VehicleType vehicleType = VehicleType.MOTORBIKE;
    private String vehicleNumber;
    private boolean active = true;
    /** Number of deliveries currently ASSIGNED or IN_TRANSIT (not stored, calculated). */
    private int openDeliveries;

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getOpenDeliveries() {
        return openDeliveries;
    }

    public void setOpenDeliveries(int openDeliveries) {
        this.openDeliveries = openDeliveries;
    }

    public String getVehicleSummary() {
        return vehicleType + " " + vehicleNumber;
    }

    @Override
    public String getRoleDescription() {
        return "Delivery agent";
    }

    @Override
    public String getDisplayName() {
        return getName() + " - " + getVehicleSummary() + " (" + openDeliveries + " open)";
    }
}
