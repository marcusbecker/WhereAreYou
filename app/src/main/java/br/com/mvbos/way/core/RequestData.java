package br.com.mvbos.way.core;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Marcus Becker on 15/08/2016.
 */
public class RequestData implements Serializable {
    public enum State {
        WAITING, SEND, ACCEPTED, PENDING, ERROR, SYNC, CANCELED;
    }

    public enum Type {
        NONE, SEND, REQUEST;
    }

    private long toNumber;
    private long fromNumber;

    private String toName;
    private String fromName;

    private double latitude;
    private double longitude;

    private String key;
    private boolean foreign;

    private Date lastUpdate = new Date();

    private State state = State.WAITING;

    private Type type = Type.NONE;

    public long getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber(toNumber);
    }

    public void setToNumber(long toNumber) {
        this.toNumber = toNumber;
    }

    public long getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(long fromNumber) {
        this.fromNumber = fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = toNumber(fromNumber);
    }

    private long toNumber(String number) {
        return Long.parseLong(number.replaceAll("[^0-9]", ""));
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isForeign() {
        return foreign;
    }

    public void setForeign(boolean foreign) {
        this.foreign = foreign;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public String getLastUpdateDescription() {
        return DateFormat.getTimeInstance().format(lastUpdate);
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getId() {
        return toNumber;
    }

    public String getFormatedLocation() {
        return String.format("Latitude %.4f, Longitude %.4f.", getLatitude(), getLongitude());
    }

    public boolean isReady() {
        return State.SYNC == state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestData that = (RequestData) o;

        if (toNumber != that.toNumber) return false;
        return fromNumber == that.fromNumber;

    }

    @Override
    public int hashCode() {
        int result = (int) (toNumber ^ (toNumber >>> 32));
        result = 31 * result + (int) (fromNumber ^ (fromNumber >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "RequestData{" +
                "toNumber='" + toNumber + '\'' +
                ", fromNumber='" + fromNumber + '\'' +
                ", toName='" + toName + '\'' +
                ", fromName='" + fromName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", state=" + state +
                '}';
    }
}
