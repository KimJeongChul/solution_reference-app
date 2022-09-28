package org.tensorflow.lite.examples.detection.inference;

import org.opencv.core.Mat;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.env.Size;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;

@Getter
@Setter
public class InferInfo {
    private long inferenceTime = 0;
    private long networkTime = 0;
    private long totalTime = 0;
    private ModelInfo modelInfo;
    private Object inferenceResult;
    private Map<String, String> responseHeaders;
    private Map<DRAW_TYPE, Object> drawable;
    private Mat frame;
    private Size previewSize;

    public boolean hasResult() {
        return this.inferenceResult != null;
    }

    public void setInferenceTime(long inferenceTime) {
        this.inferenceTime = inferenceTime;
        // Python gpu server set inference time first
        if (this.totalTime != 0) {
            this.networkTime = this.totalTime - inferenceTime;
        }
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
        // Aix server set total time first
        // because inference time can be got from postprocess
        if (this.inferenceTime != 0) {
            this.networkTime = this.totalTime - this.inferenceTime;
        } else {
            this.networkTime = 0;
            this.inferenceTime = totalTime;
        }
    }
}
