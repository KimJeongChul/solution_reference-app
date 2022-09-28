package org.tensorflow.lite.examples.detection.inference.preprocess.triton;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InputData {
    private String name;
    private List<Integer> shape;
    private String datatype;
    private InputParameters parameters;
}
