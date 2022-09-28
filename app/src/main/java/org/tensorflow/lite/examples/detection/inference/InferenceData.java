package org.tensorflow.lite.examples.detection.inference;

import android.graphics.Bitmap;

import org.tensorflow.lite.examples.detection.enums.DEVICE_TYPE;
import org.tensorflow.lite.examples.detection.enums.MODE;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.enums.VERSION;
import org.tensorflow.lite.examples.detection.env.Size;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InferenceData {
    private byte[] data;
    private Bitmap image;
    private Size previewSize;
    private int orientated; // value to rotate original image for inference
    private ModelInfo model;
    private Map<String, String> headers;

    public DEVICE_TYPE getDeviceType() {
        return model.getDeviceType();
    }

    public MODEL_NAME getModelName() {
        return model.getModelName();
    }

    public VERSION getVersion() {
        return model.getVersion();
    }

    public Size getCropSize() {
        return model.getCropSize();
    }

    public void addHeaders(Map<String, String> headers) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.putAll(headers);
    }

    public boolean isLocal() {
        return model.getMode() == MODE.LOCAL;
    }

    public boolean isMec() {
        return model.getMode() == MODE.MEC;
    }
}