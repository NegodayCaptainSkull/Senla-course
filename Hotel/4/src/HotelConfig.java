import annotations.ConfigProperty;
import annotations.ConfigClass;
import annotations.PropertyType;
import config.ConfigLoader;

import java.io.Serializable;

@ConfigClass(configFileName = "hotel.properties")
public class HotelConfig implements Serializable {
    private static final long serialVersionUID = 1000L;
    private static HotelConfig instance;

    @ConfigProperty(propertyName = "room.status.change.enabled", type = PropertyType.BOOLEAN)
    private boolean allowRoomStatusChange = true;

    @ConfigProperty(propertyName = "room.history.size", type = PropertyType.INTEGER)
    private int roomHistorySize = 3;

    private HotelConfig() {
        ConfigLoader.loadConfig(this);
    }

    public static HotelConfig getInstance() {
        if (instance == null) {
            instance = new HotelConfig();
        }
        return instance;
    }

    public boolean isAllowRoomStatusChange() { return allowRoomStatusChange; }
    public int getRoomHistorySize() { return roomHistorySize; }
}