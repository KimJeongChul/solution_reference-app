package org.tensorflow.lite.examples.detection.drawtypes;

import org.opencv.core.Point;

import java.util.List;

public class MaskRecognition {

    private List<Point[]> contourLine;
    private int colorIndex;

    public MaskRecognition(List<Point[]> contourLine, int colorIndex){
        this.contourLine = contourLine;
        this.colorIndex = colorIndex;
    }

    public int getColor() {
        return colorIndex;
    }


    public List<Point[]> getContourLine() {
        return contourLine;
    }
}
