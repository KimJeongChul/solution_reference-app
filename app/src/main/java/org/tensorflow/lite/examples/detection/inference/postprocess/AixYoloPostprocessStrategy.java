package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.drawtypes.BoxRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LabelRecognition;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.local.LocalYolov3Model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class AixYoloPostprocessStrategy extends PostprocessStrategy {

    private static final String TAG = AixYoloPostprocessStrategy.class.toString();
    private Matrix cropToFrameTransform;

    @Override
    public InferInfo postprocess(InferInfo info) {
        if (info.hasResult()) {
            cropToFrameTransform = getTransform(info);
            List<BoxRecognition> boxRecognitions = new ArrayList<>();
            List<LabelRecognition> labelRecognitions = new ArrayList<>();
            Map<DRAW_TYPE, Object> drawable = new HashMap<>();
            drawable.put(DRAW_TYPE.RECT, boxRecognitions);
            drawable.put(DRAW_TYPE.LABEL, labelRecognitions);
            info.setDrawable(drawable);

            byte[] resBytes = (byte[]) info.getInferenceResult();
            JsonParser parser = new JsonParser();
            String inferRes = info.getResponseHeaders().get("InferResponse");
            if (inferRes == null) {
                inferRes = info.getResponseHeaders().get("inferresponse");
            }

            JsonElement element = parser.parse(inferRes);
            JsonArray jsonArray = element.getAsJsonObject().get("output").getAsJsonArray();
            JsonElement output3 = jsonArray.get(0);
            JsonElement output2 = jsonArray.get(1);
            JsonElement output0 = jsonArray.get(2);
            JsonElement output1 = jsonArray.get(3);

            JsonObject obj3 = output3.getAsJsonObject().get("raw").getAsJsonObject();
            JsonObject obj2 = output2.getAsJsonObject().get("raw").getAsJsonObject();
            JsonObject obj0 = output0.getAsJsonObject().get("raw").getAsJsonObject();
            JsonObject obj1 = output1.getAsJsonObject().get("raw").getAsJsonObject();
            // if there is no boxes, return empty
            if (!obj0.has("batchByteSize")) {
                return info;
            }
            int size3 = obj3.get("batchByteSize").getAsInt();
            int size2 = obj2.get("batchByteSize").getAsInt();
            int size0 = obj0.get("batchByteSize").getAsInt();
            int size1 = obj1.get("batchByteSize").getAsInt();

            int from = 0;
            int to = size3;
            byte[] bytes3 = Arrays.copyOfRange(resBytes, from, to);

            from = to;
            to = from + size2;
            byte[] bytes2 = Arrays.copyOfRange(resBytes, from, to);

            from = to;
            to = from + size0;
            byte[] bytes0 = Arrays.copyOfRange(resBytes, from, to);

            from = to;
            to = from + size1;
            byte[] bytes1 = Arrays.copyOfRange(resBytes, from, to);

            int[] int2 = byteArrayToIntArray(bytes2);
            int[] int0 = byteArrayToIntArray(bytes0);
            float[] float1 = byteArrayToFloatArray(bytes1);
            float[] float3 = byteArrayToFloatArray(bytes3);

            Mat labels = new Mat(size2 / 4, 1, CvType.CV_32S);  // labels
            Mat boxes = new Mat(size0 / 16, 4, CvType.CV_32S); // boxes
            Mat scores = new Mat(size1 / 4, 1, CvType.CV_32F);  // scores
            labels.put(0, 0, int2);
            boxes.put(0, 0, int0);
            scores.put(0, 0, float1);
            long inferenceTime = (long) (float3[0] * 1000); // sec -> ms
            info.setInferenceTime(inferenceTime);

            for (int i = 0; i < labels.height(); i++) {
                int[] intTemp = new int[1];
                float[] floatTemp = new float[1];
                boxes.get(i, 0, intTemp);
                float top = (float) (intTemp[0]);
                boxes.get(i, 1, intTemp);
                float left = (float) (intTemp[0]);
                boxes.get(i, 2, intTemp);
                float bottom = (float) (intTemp[0]);
                boxes.get(i, 3, intTemp);
                float right = (float) (intTemp[0]);

                scores.get(i, 0, floatTemp);
                float score = floatTemp[0];
                labels.get(i, 0, intTemp);
                String objectName = LocalYolov3Model.cocoNames.get(intTemp[0]);
                int index = getColorFromMap(objectName);

                RectF rectF = new RectF(left, top, right, bottom);

                cropToFrameTransform.mapRect(rectF);
                boxRecognitions.add(new BoxRecognition(rectF, index));

                float[] labelPoint = {left, top};
                cropToFrameTransform.mapPoints(labelPoint);
                labelRecognitions.add(new LabelRecognition(objectName, score, labelPoint[0], labelPoint[1], index));
            }

        }
        return info;
    }
}
