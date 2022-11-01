package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.RectF;

public class YoloV4Recognition {
    private final String id; //  A unique identifier for what has been recognized. Specific to the class
    private final String objectName; // Display name for the recognition.
    private final float confidence; // A sortable score for how good the recognition is relative to others. Higher should be better.
    private RectF location; // Optional location within the source image for the location of the recognized object.
    private int detectedClass;

    public YoloV4Recognition(
            final String id, final String objectName, final Float confidence, final RectF location) {
        this.id = id;
        this.objectName = objectName;
        this.confidence = confidence;
        this.location = location;
    }

    public YoloV4Recognition(final String id, final String objectName, final Float confidence, final RectF location, int detectedClass) {
        this.id = id;
        this.objectName = objectName;
        this.confidence = confidence;
        this.location = location;
        this.detectedClass = detectedClass;
    }

    public String getId() {
        return id;
    }

    public String getObjectName() {
        return objectName;
    }

    public Float getConfidence() {
        return confidence;
    }

    public RectF getRect() {
        return new RectF(location);
    }

    public void setLocation(RectF location) {
        this.location = location;
    }

    public int getDetectedClass() {
        return detectedClass;
    }

    public void setDetectedClass(int detectedClass) {
        this.detectedClass = detectedClass;
    }
}

