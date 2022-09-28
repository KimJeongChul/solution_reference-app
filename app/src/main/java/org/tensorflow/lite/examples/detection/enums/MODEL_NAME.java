package org.tensorflow.lite.examples.detection.enums;

public enum MODEL_NAME {
    YOLOV2,
    YOLOV3,
    YOLOV4,
    POSENET,
    SEGMENTATION,
    SUPERNOVA;

    public boolean isYolov2() {
        return this.equals(MODEL_NAME.YOLOV2);
    }

    public boolean isYolov3() {
        return this.equals(MODEL_NAME.YOLOV3);
    }

    public boolean isPosenet() {
        return this.equals(MODEL_NAME.YOLOV3);
    }
}
