package org.tensorflow.lite.examples.detection.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestAddress {
    private String httpAddress;
    private String httpToken;
    private String grpcAddress;
    private String grpcToken;
}
