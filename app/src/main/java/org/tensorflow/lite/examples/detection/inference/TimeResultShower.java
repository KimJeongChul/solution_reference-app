package org.tensorflow.lite.examples.detection.inference;

public interface TimeResultShower {
    public void showTimeResult(long total,long inference, long network);
}
