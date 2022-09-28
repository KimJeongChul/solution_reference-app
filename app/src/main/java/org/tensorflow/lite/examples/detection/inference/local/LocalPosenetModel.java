package org.tensorflow.lite.examples.detection.inference.local;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.LinkedList;
import java.util.List;

public class LocalPosenetModel extends LocalInferenceModel {

    private Net net;

    public LocalPosenetModel(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        String protoFile = decompress("pose_deploy.prototxt");
        String modelFile = decompress("pose_iter_440000.caffemodel");
        this.net = Dnn.readNetFromCaffe(protoFile, modelFile);
    }

    @Override
    public InferInfo inference(byte[] bytes) {
        Mat imageBlob = preprocess(bytes, new Size(cropSize, cropSize), Imgproc.COLOR_RGBA2RGB,
                0.00392f);
        net.setInput(imageBlob);
        long startTime = System.currentTimeMillis();
        Mat netOutputBlob = net.forward();
        long duration = System.currentTimeMillis() - startTime;
        List<Mat> result = splitNetOutputBlobToParts(netOutputBlob, new Size(cropSize, cropSize));
        InferInfo info = new InferInfo();
        info.setInferenceTime(duration);
        info.setInferenceResult(result);
        info.setFrame(this.frame);
        return info;
    }

    private List<Mat> splitNetOutputBlobToParts(Mat netOutputBlob, Size targetSize) {

        int nParts = netOutputBlob.size(1);
        int h = netOutputBlob.size(2);

        List<Mat> netOutputParts = new LinkedList<>();
        Mat m2 = netOutputBlob.reshape(1, nParts * h);

        for (int i = 0; i < nParts; i++) {
            Mat part = m2.submat(h * i, h + h * i, 0, h);
            Mat resizedPart = new Mat();
            Imgproc.resize(part, resizedPart, targetSize);
            netOutputParts.add(resizedPart);
        }
        return netOutputParts;
    }
}
