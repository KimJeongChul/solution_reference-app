package org.tensorflow.lite.examples.detection.inference.preprocess.triton;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OutputData {
    private String name;
    private OutputParameters parameters;
}
