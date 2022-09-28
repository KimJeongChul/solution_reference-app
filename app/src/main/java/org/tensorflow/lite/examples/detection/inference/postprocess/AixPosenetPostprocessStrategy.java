package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class AixPosenetPostprocessStrategy extends PostprocessStrategy {

    private static final String TAG = AixPosenetPostprocessStrategy.class.toString();

    private final List<String> PARTNAME = Arrays.asList(
            "nose", "leftEye", "rightEye", "leftEar",
            "rightEar", "leftShoulder", "rightShoulder",
            "leftElbow", "rightElbow", "leftWrist",
            "rightWrist", "leftHip", "rightHip",
            "leftKnee", "rightKnee", "leftAnkle",
            "rightAnkle");

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
            try {
                String result = makePosenetResult(info);
                Log.e(TAG, "post result:" + result);
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
                            float y = resizedParts.get(k)[0];
                            float x = resizedParts.get(k)[1];
                            if (x > 0 && y > 0) {
                                circleRecognitions.add(new CircleRecognition(y, x));
                            }
                        }

                        for (int k = 0; k < LocalPosenetPostprocessStrategy.posePairs.length; k++) {
                            int indexA = LocalPosenetPostprocessStrategy.posePairs[k][0];
                            int indexB = LocalPosenetPostprocessStrategy.posePairs[k][1];
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

                        resizedParts.clear();

                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "## fail to parse posenet : " + e);
                return info;
            }
        }
        return info;
    }

    public String makePosenetResult(InferInfo info) throws IOException {
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

        float[] keypointCooldsRaw = byteArrayToFloatArray(bytes2);
        float[] poseScoreRaw = byteArrayToFloatArray(bytes0);
        float[] keypointScoreRaw = byteArrayToFloatArray(bytes1);
        float[] inferenceTimeRaw = byteArrayToFloatArray(bytes3);

        int poseSize = 10;
        int keypointSize = 17;

        float[][] keypointScores = new float[10][17];
        for (int j = 0; j < poseSize; j++) {
            for (int i = 0; i < keypointSize; i++) {
                keypointScores[j][i] = keypointScoreRaw[j * keypointSize + i];
            }
        }
        float[][][] keypointCoords = new float[10][17][2];
        for (int j = 0; j < poseSize; j++) {
            for (int i = 0; i < keypointSize; i++) {
                for (int k = 0; k < 2; k++) {
                    float value = keypointCooldsRaw[(j * keypointSize * 2) + (i * 2) + k];
                    keypointCoords[j][i][k] = value;
                }
            }
        }

        long inferenceTime = (long) (inferenceTimeRaw[0] * 1000); // sec -> ms
        info.setInferenceTime(inferenceTime);

        JsonObject result = new JsonObject();

        for (int i = 0; i < poseSize; i++) {
            float poseScore = poseScoreRaw[i];
            if (poseScore > 0.25) {
                JsonObject pose = new JsonObject();
                pose.addProperty("pose_score", poseScore);
                JsonObject keys = new JsonObject();
                pose.add("keys", keys);
                result.add(Integer.toString(i), pose);

                float[] keypointScore = keypointScores[i];
                for (int j = 0; j < keypointSize; j++) {
                    float keyscore = keypointScore[j];
                    String partName = PARTNAME.get(j);
                    JsonObject part = new JsonObject();
                    part.addProperty("part_confidence", keyscore);
                    keys.add(partName, part);

                    float[] coords = keypointCoords[i][j];
                    float x = coords[1];
                    float y = coords[0];

                    JsonArray array = new JsonArray();
                    array.add(y);
                    array.add(x);
                    part.add("locations", array);
                }

            }
        }

        return result.toString();
    }
}
