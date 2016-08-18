package br.com.mvbos.way.core;

import java.io.Serializable;

/**
 * Created by Marcus Becker on 15/08/2016.
 */
public class RequestData implements Serializable {
    public enum State {
        WAITING, SEND, ACCEPTED;
    }

    private String toNumber;
    private String fromNumber;

    private String toName;
    private String fromName;

    private double latitude;
    private double longitude;

    private State state = State.WAITING;

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
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

    public long getId() {
        return Long.parseLong(toNumber);
    }

    public String getFormatedLocation() {
        return String.format("Latitude %.4f, Longitude %.4f.", getLatitude(), getLongitude());
    }

    public boolean isReady() {
        return State.ACCEPTED == state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestData that = (RequestData) o;

        if (!toNumber.equals(that.toNumber)) return false;
        return fromNumber.equals(that.fromNumber);

    }

    @Override
    public int hashCode() {
        return toNumber.hashCode();
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
