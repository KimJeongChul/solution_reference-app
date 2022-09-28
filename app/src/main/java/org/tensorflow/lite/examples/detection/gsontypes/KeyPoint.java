package org.tensorflow.lite.examples.detection.gsontypes;

import androidx.annotation.NonNull;

import org.opencv.core.Point;

public class KeyPoint {

    private int id;
    private Point point;
    private float probability;

    public KeyPoint(Point point, float probability){
        this.point = point;
        this.probability = probability;
        this.id = -1;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public float getProbability() {
        return probability;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        return "id : "+ (this.id);
    }


}
