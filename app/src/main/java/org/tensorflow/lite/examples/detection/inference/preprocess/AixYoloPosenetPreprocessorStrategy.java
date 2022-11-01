package org.tensorflow.lite.examples.detection.inference.preprocess;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.dnn.Dnn;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.inference.postprocess.AixYoloPostprocessStrategy;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.InputData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.InputParameters;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.MetaData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.OutputData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.OutputParameters;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AixYoloPosenetPreprocessorStrategy extends PreprocessStrategy {
    private static final String TAG = AixYoloPosenetPreprocessorStrategy.class.toString();

    @Override
    public InferenceData preprocess(InferenceData data) {
        Bitmap image = buildBitmap(data);
        int width = data.getModel().getCropSize().width;
        int height = data.getModel().getCropSize().height;
        Size cropSize = new Size(width, height);

        byte[] body = execute(image, cropSize, data.getOrientated(),
                data.getModel().getModelName());

        Log.i(TAG, "## body=" + new String(body));

        String content = getContent(body, width, height);
        byte[] contentBytes = content.getBytes();

        Map<String, String> headers = new HashMap<>();
        headers.put("Inference-Header-Content-Length", String.valueOf(contentBytes.length));
        headers.put("InferRequest", "{ \"batch_size\": 1, \"inputs\": [{ \"name\": \"input_1:0\" " +
                "}],  \"outputs\": [{ \"name\": \"Identity:0\"}, { \"name\": \"Identity_1:0\"}, { \"name\": \"Identity_2:0\"} ] }");

        byte[] merged = new byte[body.length + contentBytes.length];

        ByteBuffer buff = ByteBuffer.wrap(merged);
        buff.put(contentBytes);
        buff.put(body);

        byte[] combined = buff.array();

        data.setHeaders(headers);
        data.setData(combined);

        return data;
    }

    public byte[] execute(Bitmap image, Size modelInputSize, int oriented, MODEL_NAME name) {
        Mat argbImage = new Mat();
        Mat rgbImage = new Mat();
        
        double modelWidth = modelInputSize.width;
        double modelHeight = modelInputSize.height;

        Utils.bitmapToMat(image, argbImage);
        Imgproc.cvtColor(argbImage, rgbImage, Imgproc.COLOR_RGBA2RGB);

        Mat resizeImage = new Mat();
        Imgproc.resize(rgbImage, resizeImage, modelInputSize, 0, 0, Imgproc.INTER_LINEAR);

        Point point = new Point(modelInputSize.width / 2, modelInputSize.height / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(point, -oriented, 1.0);
        Imgproc.warpAffine(resizeImage, resizeImage, rotImage, resizeImage.size());

        resizeImage.convertTo(resizeImage, getCvType(name)); // HWC Format
        
        int length = (int) resizeImage.total() * resizeImage.channels();
        float[] fBuffer = new float[length];
        resizeImage.get(0, 0, fBuffer);
        byte[] result_bytes = new byte[length * 4];
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(outputStream);
            for (float f: fBuffer) {
                ds.writeFloat(f);
            }
            result_bytes = outputStream.toByteArray();
            return result_bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[]{};
        }

    }

    private int getCvType(MODEL_NAME modelName) {
        if (modelName.equals(MODEL_NAME.POSENET)) {
            return CvType.CV_8UC3; // aix posenet
        } else if (modelName.equals(MODEL_NAME.YOLOV4)) {
            return CvType.CV_32FC3; // CV_32FC3
        } 
    }

    public String getContent(byte[] body, int width, int height) {
        OutputParameters outputParameters = OutputParameters.builder().binary_data(true).build();
        InputParameters inputParameters = InputParameters.builder().binary_data_size(body.length).build();

        OutputData outputData1 =
                OutputData.builder().name("Identity:0").parameters(outputParameters).build();
        OutputData outputData2 =
                OutputData.builder().name("Identity_1:0").parameters(outputParameters).build();
        OutputData outputData3 =
                OutputData.builder().name("Identity_2:0").parameters(outputParameters).build();
        InputData inputData = InputData.builder().name("input_1:0").shape(Lists.newArrayList(1,
                height, width, 3)).datatype("FP32").parameters(inputParameters).build();
        MetaData metaData =
                MetaData.builder().inputs(Lists.newArrayList(inputData)).outputs(Lists.newArrayList(outputData1, outputData2, outputData3)).build();
        Gson gson = new Gson();
        String header = gson.toJson(metaData);
        return header;
    }
}
