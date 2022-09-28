package org.tensorflow.lite.examples.detection.gsontypes;

import java.util.ArrayList;

public class SegResult {
    public ArrayList<ArrayList<Float>> boxes;
    public ArrayList<String> labels;
    public ArrayList<Float> scores;
    public float [][][][] masks;
}
