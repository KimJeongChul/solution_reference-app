package org.tensorflow.lite.examples.detection.inference.preprocess.triton;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InputParameters {
    private Integer binary_data_size;
}