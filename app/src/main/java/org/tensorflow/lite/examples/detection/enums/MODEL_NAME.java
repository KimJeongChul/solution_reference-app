package org.tensorflow.lite.examples.detection.enums;

public enum MODEL_NAME {
    YOLOV3,
    YOLOV4,
    POSENET;

    public boolean isYolov3() {
        return this.equals(MODEL_NAME.YOLOV3);
    }

    public boolean isPosenet() {
        return this.equals(MODEL_NAME.YOLOV3);
    }
}
