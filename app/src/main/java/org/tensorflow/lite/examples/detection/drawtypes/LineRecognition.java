package org.tensorflow.lite.examples.detection.drawtypes;

public class LineRecognition {
    public float startX;
    public float startY;
    public float stopX;
    public float stopY;

    public LineRecognition(float startX, float startY, float stopX, float stopY){
        this.startX = startX;
        this.startY = startY;
        this.stopX = stopX;
        this.stopY = stopY;
    }

}
