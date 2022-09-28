package org.tensorflow.lite.examples.detection.gsontypes;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YoloResults {
    public ArrayList<ArrayList<Double>> boxes;
    public ArrayList<Double> scores;
    public ArrayList<String> labels;
}
