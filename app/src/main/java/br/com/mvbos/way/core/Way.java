package br.com.mvbos.way.core;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Marcus Becker on 24/08/2016.
 */
public class Way implements Serializable {

    private String number;
    private List<RequestData> requestDatas;

    public Way() {
    }

    public Way(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public List<RequestData> getRequestDatas() {
        return requestDatas;
    }

    public void setRequestDatas(List<RequestData> requestDatas) {
        this.requestDatas = requestDatas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Way way = (Way) o;

        return number.equals(way.number);

    }

    @Override
    public int hashCode() {
        return number.hashCode();
    }

    @Override
    public String toString() {
        return "Way{" +
                "number='" + number + '\'' +
                ", requestDatas=" + requestDatas +
                '}';
    }
}

