package me.leedavison.backgroundtrial;

/**
 * Created by Leeps on 31/12/2017.
 */

public class DayListModel {

    String day;
    Double exposure;

    public DayListModel(String day, Double exposure) {
        this.day = day;
        this.exposure = exposure;
    }

    public String getDay() {
        return day;
    }

    public Double getExposure() {
        return exposure;
    }
}
