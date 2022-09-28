package org.tensorflow.lite.examples.detection.inference;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.enums.PROTOCOL;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;
import org.tensorflow.lite.examples.detection.settings.RequestAddress;
import org.tensorflow.lite.examples.detection.inference.requestor.Requestor;
import org.tensorflow.lite.examples.detection.inference.requestor.RequestorFactory;

public class RemoteInferencer implements InferenceStrategy {

    private ModelInfo modelInfo;
    private Requestor requestor;

    public RemoteInferencer(ModelInfo model) {
      setModel(model);
    }

    @Override
    public void startInference() {
        requestor.openChannel();
    }

    @Override
    public void setModel(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
        changeAddress(modelInfo.getAddress(), modelInfo.getProtocol());
    }

    public void changeAddress(RequestAddress requestAddress, PROTOCOL protocol) {
        if (this.requestor != null) {
            this.requestor.closeChannel();
        }
        this.requestor = RequestorFactory.getRequestor(protocol, requestAddress);
        this.requestor.openChannel();
    }

    //remote to local
    @Override
    public void stopInference() {
        requestor.closeChannel();
    }

    @Override
    public InferInfo inference(InferenceData data) {
        return requestor.inference(data);
    }
}
