package org.tensorflow.lite.examples.detection.drawtypes;

public class LabelRecognition {

    private String title;
    private float confidence;
    public float posX;
    public float posY;
    private int colorIndex;

    public LabelRecognition(String title, float confidence, float posX, float posY, int colorIndex){
        this.title = title;
        this.confidence = confidence;
        this.posX = posX;
        this.posY = posY;
        this.colorIndex = colorIndex;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    @Override
    public String toString() {
        String labelString =
                String.format("%s %.2f%%", title, (100 * confidence));
        return labelString.trim();
    }
}
