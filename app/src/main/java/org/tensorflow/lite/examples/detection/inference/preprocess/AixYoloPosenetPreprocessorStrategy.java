package org.tensorflow.lite.examples.detection.inference.preprocess;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.dnn.Dnn;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.tensorflow.lite.examples.detection.env.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.inference.postprocess.AixYoloPostprocessStrategy;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.InputData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.InputParameters;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.MetaData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.OutputData;
import org.tensorflow.lite.examples.detection.inference.preprocess.triton.OutputParameters;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class AixYoloPosenetPreprocessorStrategy extends PreprocessStrategy {
    private static final String TAG = AixYoloPosenetPreprocessorStrategy.class.toString();

    private static float IMAGE_MEAN = 0;
    private static float IMAGE_STD = 255.0f;

    @Override
    public InferenceData preprocess(InferenceData data) {
        Bitmap image = buildBitmap(data);
        Size previewSize = data.getPreviewSize();

        int width = data.getModel().getCropSize().width;
        int height = data.getModel().getCropSize().height;
        Size cropSize = new Size(width, height);

        byte[] body = preprocess(image, previewSize.width,
                previewSize.height, cropSize.width,
                cropSize.height, data.getOrientated());

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

    private byte[] preprocess(Bitmap bitmap, int previewWidth, int previewHeight,
                                            int modelWidth, int modelHeight, int oriented) {
        Matrix frameToCropTransform =
                ImageUtils.getTransformationMatrix(previewWidth, previewHeight, modelWidth, modelHeight,
                        oriented, false);
        Bitmap croppedBitmap = Bitmap.createBitmap(modelWidth, modelHeight,
                Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(bitmap, frameToCropTransform, null);

        // Debug for triton-image
        String root = Environment.getExternalStorageDirectory().toString();
        String dirPath = root + "/referecne_app";
        File referenceAppDir = new File(dirPath);
        if(!referenceAppDir.exists()) {
            referenceAppDir.mkdirs();
        }
        String tritionInputImageFileName = "triton-input.jpg";
        File inputImageFile = new File(referenceAppDir, tritionInputImageFileName);
        try (FileOutputStream out = new FileOutputStream(inputImageFile)) {
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch ( Exception e ) {

        }

        int imageSize = croppedBitmap.getRowBytes() * croppedBitmap.getHeight();
        ByteBuffer uncompressedBuffer = ByteBuffer.allocateDirect(imageSize);
        croppedBitmap.copyPixelsToBuffer(uncompressedBuffer);
        uncompressedBuffer.position(0);

        int channelRGBA = 4;
        int channelRGB = 3;
        int lenByteOfFloat = 4;
        int length = modelWidth * modelHeight * channelRGB * lenByteOfFloat;
        byte [] resultBytes = new byte[length];
        byte[] temp = uncompressedBuffer.array();

        String tritionInputFileName = "triton-input-v2-little-endian.buf";
        File tritionInputFile = new File(referenceAppDir, tritionInputFileName);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(outputStream);
            FileChannel outChannel = new FileOutputStream(tritionInputFile).getChannel();
            ByteBuffer outByteBuffer = ByteBuffer.allocateDirect(length);
            outByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < temp.length / channelRGBA; i++) {
                // https://stackoverflow.com/questions/18086568/android-convert-argb-8888-bitmap-to-3byte-bgr
                byte r = temp[i * 4 + 0];
                byte g = temp[i * 4 + 1];
                byte b = temp[i * 4 + 2];
                int ir = r & 0xff;
                int ig = g & 0xff;
                int ib = b & 0xff;
                float fr = (float) ir / 255;
                float fg = (float) ig / 255;
                float fb = (float) ib / 255;

                int capacity = 12;
                ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
                buffer.order(ByteOrder.LITTLE_ENDIAN); // ByteBuffer default BIG_ENDIAN
                buffer.putFloat(fr).putFloat(fg).putFloat(fb).rewind();

                outByteBuffer.putFloat(fr).putFloat(fg).putFloat(fb).rewind();

                outChannel.write(buffer);
                ds.write(buffer.array());
                buffer.flip(); 
            }
            outChannel.close();
            return outByteBuffer.array();
        } catch (Exception e) {
            Log.e(TAG, "Convert rgb byte to float exception e " + e.toString());
            return new byte[]{};
        }
    }

    private int getCvType(MODEL_NAME modelName) {
        if (modelName.equals(MODEL_NAME.POSENET)) {
            return CvType.CV_8UC3; // aix posenet
        } else if (modelName.equals(MODEL_NAME.YOLOV4)) {
            return CvType.CV_8UC3; // aix yolov4 CV_32FC3
        } else {
            return CvType.CV_8SC3; // aix yolov3
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
