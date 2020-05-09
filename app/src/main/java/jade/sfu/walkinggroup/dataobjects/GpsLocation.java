package jade.sfu.walkinggroup.dataobjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * Store information about a GPS location of a user.
 * <p>
 * WARNING: INCOMPLETE! Server returns more information than this.
 * This is just to be a placeholder and inspire you how to do it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GpsLocation {

    private double lat;
    private double lng;
    private Date timestamp;

    @Override
    public String toString() {
        return "GpsLocation{" +
                "lat=" + lat +
                ", lng=" + lng +
                ", timestamp=" + timestamp +
                '}';
    }

    private static GpsLocation instance;

    public static GpsLocation getInstance() {
        if (instance == null) {
            instance = new GpsLocation();
        }
        return instance;
    }


    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLat(double latitude) {
        this.lat = latitude;
    }

    public void setLng(double longitude) {
        this.lng = longitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}

