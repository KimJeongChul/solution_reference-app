package org.tensorflow.lite.examples.detection.queuehandler;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.InferenceData;

public interface QueueHandler {

    // 0. offer raw bytes to preprocessor from camera
    void feed(InferenceData data); // 2. preprocessor offer
    // 1. take raw bytes by preprocessor
    InferenceData getData();
    // 2. offer preprocessed bytes to inferencer
    void inference(InferenceData preprocessed);
    // 3. take preprocessed bytes from inferencer to inference
    InferenceData getPreprocessed();
    // 4. offer inference result to drawer
    void draw(InferInfo info);
    // 5. take inference result to draw
    InferInfo getInferenced();

    void waiting();

    void wake();

    void clear();
}

