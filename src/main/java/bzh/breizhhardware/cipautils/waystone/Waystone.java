package bzh.breizhhardware.cipautils.waystone;

import org.bukkit.Location;

public class Waystone {
    private final String id;
    private String name;
    private final String owner;
    private final Location location;
    private boolean isPublic;

    public Waystone(String id, String name, String owner, Location location, boolean isPublic) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.location = location;
        this.isPublic = isPublic;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Override
    public String toString() {
        return "Waystone{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", location=" + location +
                ", isPublic=" + isPublic +
                '}';
    }
}
