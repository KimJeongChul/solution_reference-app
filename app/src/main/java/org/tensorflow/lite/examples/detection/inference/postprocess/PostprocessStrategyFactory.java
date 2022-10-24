package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.util.Log;

import org.tensorflow.lite.examples.detection.enums.DEVICE_TYPE;
import org.tensorflow.lite.examples.detection.enums.MODE;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;

public class PostprocessStrategyFactory {
    private static final String TAG = PostprocessStrategyFactory.class.toString();
    public static PostprocessStrategy getPostprocessor(ModelInfo info) {
        MODEL_NAME model = info.getModelName();
        MODE mode = info.getMode();
        DEVICE_TYPE device = info.getDeviceType();
        if (mode.equals(MODE.LOCAL)) {
            if (model.equals(MODEL_NAME.YOLOV2) || model.equals(MODEL_NAME.YOLOV3) || model.equals(MODEL_NAME.YOLOV4)) {
                return new LocalYoloPostprocessStrategy();
            } else if (model.equals(MODEL_NAME.POSENET)) {
                return new LocalPosenetPostprocessStrategy();
            } else {
                throw new RuntimeException(String.format("Not supported model name:%s",
                        info.getModelName().toString()));
            }
        } else {
            if (device.equals(DEVICE_TYPE.GPU)) {
                if (model.equals(MODEL_NAME.YOLOV2) || model.equals(MODEL_NAME.YOLOV3)) {
                    return new GpuYoloPostprocessStrategy();
                } else if (model.equals(MODEL_NAME.POSENET)) {
                    return new GpuPosenetPostprocessStrategy();
                } else {
                    throw new RuntimeException(String.format("Not supported model name:%s",
                            info.getModelName().toString()));
                }
            } else if (device.equals(DEVICE_TYPE.AIX)) {
                if (model.equals(MODEL_NAME.YOLOV2) || model.equals(MODEL_NAME.YOLOV3)) {
                    return new AixYoloPostprocessStrategy();
                } else if (model.equals(MODEL_NAME.POSENET)) {
                    return new AixPosenetPostprocessStrategy();
                } else {
                    throw new RuntimeException(String.format("Not supported model name:%s",
                            info.getModelName().toString()));
                }
            } else {
                throw new RuntimeException(String.format("Not supported device type:%s",
                        info.getDeviceType().toString()));
            }
        }
    }
}
