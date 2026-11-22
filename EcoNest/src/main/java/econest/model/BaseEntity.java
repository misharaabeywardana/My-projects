package econest.model;

public abstract class BaseEntity {

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /** True when the object has not been saved to the database yet. */
    public boolean isNew() {
        return id <= 0;
    }

    /** Short text used in combo boxes and messages. */
    public abstract String getDisplayName();

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) other;
        return id > 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return id > 0 ? Integer.hashCode(id) : System.identityHashCode(this);
    }
}
