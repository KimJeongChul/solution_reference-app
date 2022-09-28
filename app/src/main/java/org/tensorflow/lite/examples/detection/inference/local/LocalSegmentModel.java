package org.tensorflow.lite.examples.detection.inference.local;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.dnn.Dnn.DNN_BACKEND_OPENCV;
import static org.opencv.dnn.Dnn.DNN_TARGET_CPU;

public class LocalSegmentModel extends LocalInferenceModel {

    private Net net;

    public LocalSegmentModel(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        String cfgFile = decompress("mask_rcnn_inception.pbtxt");
        String modelFile = decompress("frozen_inference_graph.pb");

        net = Dnn.readNetFromTensorflow(modelFile, cfgFile);
        net.setPreferableBackend(DNN_BACKEND_OPENCV);
        net.setPreferableTarget(DNN_TARGET_CPU);
    }

    @Override
    public InferInfo inference(byte[] bytes) {
        Mat imageBlob = preprocess(bytes, new Size(cropSize, cropSize), Imgproc.COLOR_RGBA2BGR,
                1.0f);
        net.setInput(imageBlob);
        long startTime = System.currentTimeMillis();
        List<String> outNames = new ArrayList<>(2);
        outNames.add("detection_out_final");
        outNames.add("detection_masks");
        List<Mat> result = new ArrayList<>();
        net.forward(result, outNames);
        long duration = System.currentTimeMillis() - startTime;
        InferInfo info = new InferInfo();
        info.setInferenceTime(duration);
        info.setInferenceResult(result);
        info.setFrame(this.frame);
        return info;
    }
}
