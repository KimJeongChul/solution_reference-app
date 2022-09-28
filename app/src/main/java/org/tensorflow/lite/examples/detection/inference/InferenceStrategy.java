package org.tensorflow.lite.examples.detection.inference;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;

import java.io.IOException;


public interface InferenceStrategy {
     public InferInfo inference(InferenceData preprocessed) throws IOException;
     public void stopInference();
     public void startInference();
     public void setModel(ModelInfo modelInfo);
}
