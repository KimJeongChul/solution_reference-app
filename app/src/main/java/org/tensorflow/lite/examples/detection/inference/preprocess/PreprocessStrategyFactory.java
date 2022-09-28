package org.tensorflow.lite.examples.detection.inference.preprocess;

import org.tensorflow.lite.examples.detection.enums.DEVICE_TYPE;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.inference.InferenceData;

public class PreprocessStrategyFactory {
    private static final String TAG = PreprocessStrategyFactory.class.toString();

    public static PreprocessStrategy getPreprocessor(InferenceData data) {
        if (data.isLocal() || data.getDeviceType() == DEVICE_TYPE.GPU) {
            return new LocalGPUPreprocessorStrategy();
        } else if (data.isMec() &&
                (data.getModelName() == MODEL_NAME.POSENET ||
                        data.getModelName() == MODEL_NAME.YOLOV3 ||
                        data.getModelName() == MODEL_NAME.YOLOV2)) {
            return new AixYoloPosenetPreprocessorStrategy();
        } else if (data.isMec() && data.getModelName() == MODEL_NAME.SUPERNOVA) {
            return new AixSupernovaPreprocessorStarategy();
        } else {
            return null;
        }
    }
}
