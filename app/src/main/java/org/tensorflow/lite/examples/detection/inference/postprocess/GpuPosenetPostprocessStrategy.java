package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.gsontypes.KeyPointEnum;
import org.tensorflow.lite.examples.detection.gsontypes.PersonwiseResult;
import org.tensorflow.lite.examples.detection.drawtypes.CircleRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LineRecognition;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GpuPosenetPostprocessStrategy extends PostprocessStrategy {

    private static final String TAG = GpuPosenetPostprocessStrategy.class.toString();

    private int[][] posePairs = {
            {1, 2}, {1, 5}, {2, 3}, {3, 4}, {5, 6}, {6, 7},
            {1, 8}, {8, 9}, {9, 10}, {1, 11}, {11, 12}, {12, 13},
            {1, 0}, {0, 14}, {14, 16}, {0, 15}, {15, 17}
    };

    private Matrix cropToFrameTransform;

    public float[] initializePoint(List<Float> location) {
        float x = location.get(1);
        float y = location.get(0);
        //reversed in server

        float[] point = {x, y};
        if (x > 0 && y > 0) {
            cropToFrameTransform.mapPoints(point);
        }
        return point;
    }

    @Override
    public InferInfo postprocess(InferInfo info) {
        if (info.hasResult()) {
            String result = (String) info.getInferenceResult();
            cropToFrameTransform = getTransform(info);
            List<CircleRecognition> circleRecognitions = new ArrayList<>();
            List<LineRecognition> lineRecognitions = new ArrayList<>();
            Map<DRAW_TYPE, Object> drawable = new HashMap<>();
            drawable.put(DRAW_TYPE.CIRCLE, circleRecognitions);
            drawable.put(DRAW_TYPE.LINE, lineRecognitions);
            info.setDrawable(drawable);

            List<float[]> resizedParts = new ArrayList<>(18);
            if (result != "") {
                JSONArray array = new JSONArray();
                try {
                    JSONObject obj = new JSONObject(result);
                    Iterator x = obj.keys();
                    while (x.hasNext()) {
                        String key = (String) x.next();
                        array.put(obj.get(key));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String jsonArray = array.toString();

                Gson gson = new Gson();
                Type type = new TypeToken<List<PersonwiseResult>>() {
                }.getType();
                List<PersonwiseResult> people = gson.fromJson(jsonArray, type);

                for (final PersonwiseResult person : people) {
                    KeyPointEnum keys = person.getKeys();

                    resizedParts.add(0, initializePoint(keys.getNose().getLocations()));// nose = 0
                    resizedParts.add(1, new float[]{0});
                    resizedParts.add(2, initializePoint(keys.getRightShoulder().getLocations()));//R-Sho = 2
                    resizedParts.add(3, initializePoint(keys.getRightElbow().getLocations()));//R-Elb = 3
                    resizedParts.add(4, initializePoint(keys.getRightWrist().getLocations()));//R-wr = 4
                    resizedParts.add(5, initializePoint(keys.getLeftShoulder().getLocations()));//L-Sho = 5
                    float[] neckPoint = {0, 0};
                    resizedParts.set(1, neckPoint);// neck = 1
                    resizedParts.add(6, initializePoint(keys.getLeftElbow().getLocations()));//L-Elb = 6
                    resizedParts.add(7, initializePoint(keys.getLeftWrist().getLocations()));//L-Wr = 7
                    resizedParts.add(8, initializePoint(keys.getRightHip().getLocations())); // R-Hip = 8
                    resizedParts.add(9, initializePoint(keys.getRightKnee().getLocations())); // R-knee = 9
                    resizedParts.add(10, initializePoint(keys.getRightAnkle().getLocations())); //R-ank = 10
                    resizedParts.add(11, initializePoint(keys.getLeftHip().getLocations()));//L-hip = 11
                    resizedParts.add(12, initializePoint(keys.getLeftKnee().getLocations()));//L-knee =12
                    resizedParts.add(13, initializePoint(keys.getLeftAnkle().getLocations()));//L-Ank = 13
                    resizedParts.add(14, initializePoint(keys.getRightEye().getLocations()));//R-Eye = 14
                    resizedParts.add(15, initializePoint(keys.getLeftEye().getLocations()));//L-Eye = 15
                    resizedParts.add(16, initializePoint(keys.getRightEar().getLocations()));//R-Ear = 16
                    resizedParts.add(17, initializePoint(keys.getLeftEar().getLocations()));//L-Ear = 17

                    for (int k = 0; k < resizedParts.size(); k++) {
                        circleRecognitions.add(
                                new CircleRecognition(resizedParts.get(k)[0], resizedParts.get(k)[1]));
                    }

                    for (int k = 0; k < posePairs.length; k++) {
                        int indexA = posePairs[k][0];
                        int indexB = posePairs[k][1];
                        if (resizedParts.size() > 0) {
                            float[] startPoint = resizedParts.get(indexA);
                            float[] stopPoint = resizedParts.get(indexB);
                            float startx = startPoint[1];
                            float starty = startPoint[0];
                            float stopx = stopPoint[1];
                            float stopy = stopPoint[0];
                            if (startx > 0 && starty > 0 && stopx > 0 && stopy > 0) {
                                lineRecognitions.add(
                                        new LineRecognition(starty, startx, stopy, stopx));
                            }
                        }
                    }
                }
            }
        }
        return info;
    }
}
