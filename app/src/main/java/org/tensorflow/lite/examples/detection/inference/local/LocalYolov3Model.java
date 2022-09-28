package org.tensorflow.lite.examples.detection.inference.local;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalYolov3Model extends LocalInferenceModel {
    public static List<String> cocoNames = Arrays.asList(
            "person", "bicycle", "motorbike", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "car", "bench",
            "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe",
            "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard",
            "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl",
            "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza",
            "donut", "cake", "chair", "sofa", "pottedplant", "bed", "diningtable", "toilet",
            "tvmonitor", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
            "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors",
            "teddy bear", "hair drier", "toothbrush");
    private Net net;

    public LocalYolov3Model(Context context) {
        super(context);
        initialize();
    }

    @Override
    public InferInfo inference(byte[] bytes) {
        Mat imageBlob = preprocess(bytes, new Size(cropSize, cropSize),
                Imgproc.COLOR_RGBA2RGB, 0.00392f);
        net.setInput(imageBlob);
        List<Mat> result = new ArrayList<>();
        List<String> outBlobNames = getOutputNames(net);
        long startTime = System.currentTimeMillis();
        net.forward(result, outBlobNames);
        long duration = System.currentTimeMillis() - startTime;
        InferInfo info = new InferInfo();
        info.setInferenceTime(duration);
        info.setInferenceResult(result);
        info.setFrame(this.frame);
        return info;
    }

    private static List<String> getOutputNames(Net net) {
        List<String> names = new ArrayList<>();
        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String> layersNames = net.getLayerNames();
        outLayers.forEach((item) -> names.add(layersNames.get(item - 1)));
        return names;
    }

    private void initialize() {

        String cfgFile = decompress("yolov4.cfg");
        String weightsFile = decompress("yolov4.weights");

        this.net = Dnn.readNetFromDarknet(cfgFile, weightsFile);
    }
}
