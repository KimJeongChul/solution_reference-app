package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.graphics.RectF;

import com.google.gson.Gson;

import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.env.Size;
import org.tensorflow.lite.examples.detection.gsontypes.YoloResults;
import org.tensorflow.lite.examples.detection.drawtypes.BoxRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LabelRecognition;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GpuYoloPostprocessStrategy extends PostprocessStrategy {
    private Matrix cropToFrameTransform;

    @Override
    public InferInfo postprocess(InferInfo info) {
        if (info.hasResult()) {
            Size cropSize = info.getModelInfo().getCropSize();
            // preview can be difference, so always create new transform
            cropToFrameTransform = getTransform(info);
            String result = (String) info.getInferenceResult();
            List<BoxRecognition> boxRecognitions = new ArrayList<>();
            List<LabelRecognition> labelRecognitions = new ArrayList<>();
            Map<DRAW_TYPE, Object> drawable = new HashMap<>();
            drawable.put(DRAW_TYPE.RECT, boxRecognitions);
            drawable.put(DRAW_TYPE.LABEL, labelRecognitions);
            info.setDrawable(drawable);

            Gson gson = new Gson();
            YoloResults gsonResults = gson.fromJson(result, YoloResults.class);
            if (gsonResults.labels != null) {
                for (int i = 0; i < gsonResults.labels.size(); i++) {
                    float top = gsonResults.boxes.get(i).get(0).floatValue();
                    float left = gsonResults.boxes.get(i).get(1).floatValue();
                    float bottom = gsonResults.boxes.get(i).get(2).floatValue();
                    float right = gsonResults.boxes.get(i).get(3).floatValue();

                    top = Math.max(0, (float) Math.floor(top + 0.5));
                    left = Math.max(0, (float) Math.floor(left + 0.5));
                    bottom = Math.min(cropSize.height, (float) Math.floor(bottom + 0.5));
                    right = Math.min(cropSize.width, (float) Math.floor(right + 0.5));

                    float score = gsonResults.scores.get(i).floatValue();

                    String objectName = gsonResults.labels.get(i);

                    int index = getColorFromMap(objectName);

                    RectF rectF = new RectF(left, top, right, bottom);
                    cropToFrameTransform.mapRect(rectF);

                    boxRecognitions.add(new BoxRecognition(rectF, index));
                    float[] labelPoint = {left, top};
                    cropToFrameTransform.mapPoints(labelPoint);
                    labelRecognitions.add(new LabelRecognition(gsonResults.labels.get(i), score, labelPoint[0], labelPoint[1], index));
                }
            }
        }
        return info;
    }
}
