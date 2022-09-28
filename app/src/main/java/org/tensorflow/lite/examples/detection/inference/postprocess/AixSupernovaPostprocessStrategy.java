package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.Gson;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.postprocess.triton.ResponseContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class AixSupernovaPostprocessStrategy extends PostprocessStrategy {
    private static String TAG = AixSupernovaPostprocessStrategy.class.toString();

    @Override
    public InferInfo postprocess(InferInfo info) {
        Log.i(TAG, String.format("## has result:%s, info:%s", info.hasResult(), info));
        if (info.hasResult()) {
            Log.i(TAG, String.format("## before response headers:%s", info.getResponseHeaders()));
            byte[] resBytes = (byte[]) info.getInferenceResult();
            Log.i(TAG, String.format(TAG, "## response bytes length:%s", resBytes.length));

            Integer contentLength = Integer.valueOf(info.getResponseHeaders().get(
                    "inference-header-content-length"));
            Log.i(TAG, String.format("## content length:%s", contentLength));
            byte[] contentBytes = Arrays.copyOfRange(resBytes, 0, contentLength);
            String contentStr = new String(contentBytes);
            Log.i(TAG, String.format("response content:%s", contentStr));
            Gson gson = new Gson();
            ResponseContent content = gson.fromJson(contentStr, ResponseContent.class);
            Integer bodyLength = content.getOutputs().get(0).getParameters().getBinary_data_size();
            byte[] body = Arrays.copyOfRange(resBytes, contentLength, bodyLength);
            int outputWidth = info.getModelInfo().getCropSize().width * 2;
            int outputHeight = info.getModelInfo().getCropSize().height * 2;
            int osize = outputWidth * outputHeight;

            byte[] rbytes = Arrays.copyOfRange(body, 0, osize);
            byte[] gbytes = Arrays.copyOfRange(body, osize, osize * 2);
            byte[] bbytes = Arrays.copyOfRange(body, osize * 2, osize * 3);

            Mat rmat = new Mat(outputHeight, outputWidth, CvType.CV_8U);
            rmat.put(0, 0, rbytes);
            Mat gmat = new Mat(outputHeight, outputWidth, CvType.CV_8U);
            gmat.put(0, 0, gbytes);
            Mat bmat = new Mat(outputHeight, outputWidth, CvType.CV_8U);
            bmat.put(0, 0, bbytes);
            List<Mat> rgb = new ArrayList<>();
            rgb.add(rmat);
            rgb.add(gmat);
            rgb.add(bmat);

            Mat rgbmat = new Mat(outputHeight, outputWidth, CvType.CV_8UC3);
            Core.merge(rgb, rgbmat);
            Log.i(TAG, String.format("## rgb map width:%s, height:%s, channel:%s, dims:%s",
                    rgbmat.width()
                    , rgbmat.height(), rgbmat.channels(), rgbmat.dims()));

            Bitmap result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888);
            Log.i(TAG, String.format("## bitmap width:%s, height:%s", result.getWidth(),
                    result.getHeight()));
            Utils.matToBitmap(rgbmat, result);

            Map<DRAW_TYPE, Object> drawable = new HashMap<>();
            drawable.put(DRAW_TYPE.IMAGE, result);
            info.setDrawable(drawable);
        }
        return info;
    }
}
