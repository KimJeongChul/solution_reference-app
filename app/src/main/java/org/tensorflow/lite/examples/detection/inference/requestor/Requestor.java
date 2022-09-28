package org.tensorflow.lite.examples.detection.inference.requestor;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

public interface Requestor {
    public InferInfo inference(InferenceData data);
    public void openChannel();
    public void closeChannel();
}


