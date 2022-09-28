package org.tensorflow.lite.examples.detection.inference.requestor;

import org.tensorflow.lite.examples.detection.enums.PROTOCOL;
import org.tensorflow.lite.examples.detection.settings.RequestAddress;

public class RequestorFactory {
    public static Requestor getRequestor(PROTOCOL protocol, RequestAddress address) {
        if (protocol.equals(PROTOCOL.GRPC)) {
            return new GrpcRequestor(address);
        } else if (protocol.equals(PROTOCOL.HTTP)) {
             return new HttpRequestor(address);
        } else {
            throw new RuntimeException("not support protocol");
        }
    }
}
