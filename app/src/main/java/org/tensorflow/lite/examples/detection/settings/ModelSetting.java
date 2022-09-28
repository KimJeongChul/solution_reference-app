package org.tensorflow.lite.examples.detection.settings;

import org.tensorflow.lite.examples.detection.enums.COLOR_FORMAT;
import org.tensorflow.lite.examples.detection.enums.FRAME_CONTROL;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.enums.PROTOCOL;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelSetting {
    private MODEL_NAME modelName;
    private boolean irtDisplay;
    private boolean modelNameDisplay;
    private FRAME_CONTROL frameControl;
    private String address;
    private String token;
    private COLOR_FORMAT colorFormat;
    private PROTOCOL protocol;
}
