package org.tensorflow.lite.examples.detection.inference.preprocess;

import android.graphics.Bitmap;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.InputData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.InputParameters;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.MetaData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.OutputData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.OutputParameters;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


public class AixSupernovaPreprocessorStarategy extends PreprocessStrategy {

    @Override
    public InferenceData preprocess(InferenceData data) {
        Bitmap image = buildBitmap(data);
        int width = data.getModel().getCropSize().width;
        int height = data.getModel().getCropSize().height;
        Size cropSize = new Size(width, height);
        byte[] body = execute(image, cropSize, data.getOrientated(),
                data.getModel().getModelName());

        String content = getContent(body, width, height) ;
        byte[] contentBytes = content.getBytes();

        Map<String, String> headers = new HashMap<>();
        headers.put("Inference-Header-Content-Length", String.valueOf(contentBytes.length));
        headers.put("InferRequest", "{ \"batch_size\": 1, \"inputs\": [{ \"name\": \"DATA_IN\" " +
                "}],  \"outputs\": [{ \"name\": \"DATA_OUT\"} ] }");

        byte[] merged = new byte[body.length + contentBytes.length];

        ByteBuffer buff = ByteBuffer.wrap(merged);
        buff.put(contentBytes);
        buff.put(body);

        byte[] combined = buff.array();

        data.setHeaders(headers);
        data.setData(combined);
        return data;
    }

    public byte[] execute(Bitmap image, Size newSize, int oriented, MODEL_NAME name) {
        Mat argbImage = new Mat();
        Mat rgbImage = new Mat();

        Utils.bitmapToMat(image, argbImage);
        Imgproc.cvtColor(argbImage, rgbImage, Imgproc.COLOR_RGBA2RGB);
        //Mat resizeImage = resizeKeepAspectRatio(rgbImage, newSize, new Scalar(0,0,0));
        Mat resizeImage = new Mat();
        Imgproc.resize(rgbImage, resizeImage, newSize, 0, 0, Imgproc.INTER_LINEAR);

        Point point = new Point(newSize.width / 2, newSize.height / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(point, -oriented, 1.0);
        Imgproc.warpAffine(resizeImage, resizeImage, rotImage, resizeImage.size());

        // need to test for aix yolo
        resizeImage.convertTo(resizeImage, CvType.CV_8UC3);

        byte[] return_bytes = new byte[
                (int) (resizeImage.width() * resizeImage.height() * resizeImage.channels())];
        resizeImage.get(0, 0, return_bytes);

        return return_bytes;
    }

    public String getContent(byte[] body, int width, int height) {
        OutputParameters outputParameters = OutputParameters.builder().binary_data(true).build();
        InputParameters inputParameters = InputParameters.builder().binary_data_size(body.length).build();

        OutputData outputData =
                OutputData.builder().name("DATA_OUT").parameters(outputParameters).build();
        InputData inputData = InputData.builder().name("DATA_IN").shape(Lists.newArrayList(1,
                height, width, 3)).datatype("UINT8").parameters(inputParameters).build();
        MetaData metaData =
                MetaData.builder().inputs(Lists.newArrayList(inputData)).outputs(Lists.newArrayList(outputData)).build();
        Gson gson = new Gson();
        String header = gson.toJson(metaData);
        return header;
    }
}
