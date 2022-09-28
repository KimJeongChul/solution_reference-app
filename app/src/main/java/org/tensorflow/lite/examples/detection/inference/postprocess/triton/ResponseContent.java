package org.tensorflow.lite.examples.detection.inference.postprocess.triton;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseContent {
    private String model_name;
    private String model_version;
    private List<ResponseOutput> outputs;
}
