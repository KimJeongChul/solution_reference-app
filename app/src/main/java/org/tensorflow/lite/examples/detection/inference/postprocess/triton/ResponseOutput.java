package org.tensorflow.lite.examples.detection.inference.postprocess.triton;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseOutput {
    private String name;
    private String datatype;
    private List<Integer> shape;
    private ResponseOutputParameters parameters;
}
