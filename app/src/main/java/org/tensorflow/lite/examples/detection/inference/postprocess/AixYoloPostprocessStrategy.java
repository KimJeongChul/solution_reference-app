package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.drawtypes.BoxRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LabelRecognition;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.local.LocalYolov4Model;
import org.tensorflow.lite.examples.detection.inference.postprocess.triton.ResponseContent;
import org.tensorflow.lite.examples.detection.env.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class AixYoloPostprocessStrategy extends PostprocessStrategy {

    private static final String TAG = AixYoloPostprocessStrategy.class.toString();

    private Matrix cropToFrameTransform;

    private static final int INPUT_SIZE = 416;
    private static final int[] ANCHORS = new int[]{
            12, 16, 19, 36, 40, 28, 36, 75, 76, 55, 72, 146, 142, 110, 192, 243, 459, 401
    };
    private static final float[] XYSCALE = new float[]{1.2f, 1.1f, 1.05f};
    private static final int[][] MASKS = new int[][]{{0, 1, 2}, {3, 4, 5}, {6, 7, 8}};

    private static final float CONFIDENCE_THRESH_HOLD = 0.5f;
    private static final float NMS_THRESH_HOLd = 0.6f;
    private static final int NUM_BOXES_PER_BLOCK = 3;

    protected float box_iou(RectF a, RectF b) {
        return box_intersection(a, b) / box_union(a, b);
    }

    protected float box_intersection(RectF a, RectF b) {
        float w = overlap((a.left + a.right) / 2, a.right - a.left,
                (b.left + b.right) / 2, b.right - b.left);
        float h = overlap((a.top + a.bottom) / 2, a.bottom - a.top,
                (b.top + b.bottom) / 2, b.bottom - b.top);
        if (w < 0 || h < 0) return 0;
        float area = w * h;
        return area;
    }

    protected float box_union(RectF a, RectF b) {
        float i = box_intersection(a, b);
        float u = (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i;
        return u;
    }

    protected float overlap(float x1, float w1, float x2, float w2) {
        float l1 = x1 - w1 / 2;
        float l2 = x2 - w2 / 2;
        float left = l1 > l2 ? l1 : l2;
        float r1 = x1 + w1 / 2;
        float r2 = x2 + w2 / 2;
        float right = r1 < r2 ? r1 : r2;
        return right - left;
    }

    protected ArrayList<YoloV4Recognition> nms(ArrayList<YoloV4Recognition> inputList) {
        ArrayList<YoloV4Recognition> nmsList = new ArrayList<YoloV4Recognition>();

        for (int k = 0; k < LocalYolov4Model.cocoNames.size(); k++) {
            //1.find max confidence per class
            PriorityQueue<YoloV4Recognition> pq =
                    new PriorityQueue<YoloV4Recognition>(
                            50,
                            new Comparator<YoloV4Recognition>() {
                                @Override
                                public int compare(final YoloV4Recognition lhs, final YoloV4Recognition rhs) {
                                    // Intentionally reversed to put high confidence at the head of the queue.
                                    return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                                }
                            });
            for (int i = 0; i < inputList.size(); ++i) {
                if (inputList.get(i).getConfidence() > 1) {
                    continue;
                }
                if (inputList.get(i).getDetectedClass() == k) {
                    pq.add(inputList.get(i));
                }
            }

            //2.do non maximum suppression
            while (pq.size() > 0) {
                //insert detection with max confidence
                YoloV4Recognition[] a = new YoloV4Recognition[pq.size()];
                YoloV4Recognition[] detections = pq.toArray(a);
                YoloV4Recognition max = detections[0];
                nmsList.add(max);
                pq.clear();

                for (int j = 1; j < detections.length; j++) {
                    YoloV4Recognition detection = detections[j];
                    RectF b = detection.getRect();
                    if (box_iou(max.getRect(), b) < NMS_THRESH_HOLd) {
                        pq.add(detection);
                    }
                }
            }
        }
        return nmsList;
    }

    private static float expit(final float x) {
        return (float) (1. / (1. + Math.exp(-x)));
    }

    private float[][][][][] byteArryToOutputFloatArray(JsonArray shape, byte[] buf) {
        // [1][LAYER_WIDTH][LAYER_WIDTH][BOX_PER_BlOCK][NUM_OF_CLASS]
        int layerWidth = shape.get(1).getAsInt();
        int boxPerBlock = shape.get(3).getAsInt();
        int numOfClass = shape.get(4).getAsInt();

        int offset = 0;

        float[][][][][] fout = new float[1][layerWidth][layerWidth][boxPerBlock][numOfClass];

        for (int y = 0; y < layerWidth; ++y ) {
            for (int x = 0; x < layerWidth; ++x) {
                for (int b = 0; b < boxPerBlock; ++b) {
                    for (int l = 0; l < numOfClass; ++l) {
                        float f = byteToFloat(Arrays.copyOfRange(buf, offset, offset + 4));
                        fout[0][y][x][b][l] = f;
                        offset += 4;
                    }
                }
            }
        }

        return fout;
    }

    @Override
    public InferInfo postprocess(InferInfo info) {
        if (info.hasResult()) {
            Size cropSize = getPreviewSize(info);
            cropToFrameTransform = getTransform(info);
            List<BoxRecognition> boxRecognitions = new ArrayList<>();
            List<LabelRecognition> labelRecognitions = new ArrayList<>();
            Map<DRAW_TYPE, Object> drawable = new HashMap<>();
            drawable.put(DRAW_TYPE.RECT, boxRecognitions);
            drawable.put(DRAW_TYPE.LABEL, labelRecognitions);
            info.setDrawable(drawable);

            byte[] resBytes = (byte[]) info.getInferenceResult();
            Log.i(TAG, String.format(TAG, "## response bytes length:%s", resBytes.length));

            Integer contentLength = Integer.valueOf(info.getResponseHeaders().get(
                    "inference-header-content-length"));
            Log.i(TAG, String.format("## content length:%s", contentLength));
            byte[] contentBytes = Arrays.copyOfRange(resBytes, 0, contentLength);
            String contentStr = new String(contentBytes);
            Log.i(TAG, String.format("## content str:%s", contentStr));

            Gson gson = new Gson();
            ResponseContent content = gson.fromJson(contentStr, ResponseContent.class);
            Integer bodyLength = content.getOutputs().get(0).getParameters().getBinary_data_size();

            byte[] header = Arrays.copyOfRange(resBytes, 0, contentLength); // split [:contentLength]
            byte[] body = Arrays.copyOfRange(resBytes, contentLength, bodyLength); // split [:contentLength]

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(new String(header));
            JsonArray jsonArray = element.getAsJsonObject().get("outputs").getAsJsonArray();
            JsonElement jsonOutput1 = jsonArray.get(0);
            JsonElement jsonOutput2 = jsonArray.get(1);
            JsonElement jsonOutput3 = jsonArray.get(2);

            JsonArray output1Shape = jsonOutput1.getAsJsonObject().get("shape").getAsJsonArray();
            JsonArray output2Shape = jsonOutput2.getAsJsonObject().get("shape").getAsJsonArray();
            JsonArray output3Shape = jsonOutput3.getAsJsonObject().get("shape").getAsJsonArray();

            JsonObject obj1 = jsonOutput1.getAsJsonObject().get("parameters").getAsJsonObject();
            JsonObject obj2 = jsonOutput2.getAsJsonObject().get("parameters").getAsJsonObject();
            JsonObject obj3 = jsonOutput3.getAsJsonObject().get("parameters").getAsJsonObject();

            int size1 = obj1.get("binary_data_size").getAsInt(); // 13 * 13 * 3 * 85
            int size2 = obj2.get("binary_data_size").getAsInt(); // 26 * 26 * 3 * 85
            int size3 = obj3.get("binary_data_size").getAsInt(); // 52 * 52 * 3 * 85
            int byteLength = size1 + size2 + size3;

            int from = 0;
            int to = size1;
            byte[] output1 = Arrays.copyOfRange(resBytes, from, to);

            from = to;
            to = from + size2;
            byte[] output2 = Arrays.copyOfRange(resBytes, from, to);

            from = to;
            to = from + size3;
            byte[] output3 = Arrays.copyOfRange(resBytes, from, to);

            Map<Integer, Object> outputMap = new HashMap<>();
            ArrayList<Integer> outputLayoutWidthList = new ArrayList<Integer>();

            int output1LayerWidth = output1Shape.get(1).getAsInt();
            outputLayoutWidthList.add(output1LayerWidth);

            int output2LayerWidth = output2Shape.get(1).getAsInt();
            outputLayoutWidthList.add(output2LayerWidth);

            int output3LayerWidth = output3Shape.get(1).getAsInt();
            outputLayoutWidthList.add(output3LayerWidth);

            outputMap.put(0, byteArryToOutputFloatArray(output1Shape, output1));
            outputMap.put(1, byteArryToOutputFloatArray(output2Shape, output2));
            outputMap.put(2, byteArryToOutputFloatArray(output3Shape, output3));

            int labelSize = LocalYolov4Model.cocoNames.size(); // 80 class

            // Insert Recognition to all of detections
            ArrayList<YoloV4Recognition> detections = new ArrayList<YoloV4Recognition>();
            for (int i = 0; i < outputLayoutWidthList.size(); i++) {
                int gridWidth = outputLayoutWidthList.get(i);
                float[][][][][] out = (float[][][][][]) outputMap.get(i);

                for (int y = 0; y < gridWidth; ++y) {
                    for (int x = 0; x < gridWidth; ++x) {
                        for (int b = 0; b < NUM_BOXES_PER_BLOCK; ++b) {

                            final int offset =
                                    (gridWidth * (NUM_BOXES_PER_BLOCK * (labelSize + 5))) * y
                                            + (NUM_BOXES_PER_BLOCK * (labelSize + 5)) * x
                                            + (labelSize + 5) * b;

                            final float confidence = expit(out[0][y][x][b][4]);
                            int detectedClass = -1;
                            float maxClass = 0;

                            // Get prediction class highest score
                            final float[] classes = new float[labelSize];
                            for (int c = 0; c < labelSize; ++c) {
                                classes[c] = out[0][y][x][b][5 + c];
                            }
                            for (int c = 0; c < labelSize; ++c) {
                                if (classes[c] > maxClass) {
                                    detectedClass = c;
                                    maxClass = classes[c];
                                }
                            }

                            String objectName = LocalYolov4Model.cocoNames.get(detectedClass);

                            final float confidenceInClass = maxClass * confidence;
                            if (confidenceInClass > CONFIDENCE_THRESH_HOLD) {
                                final float xPos = (x + (expit(out[0][y][x][b][0]) * XYSCALE[i]) - (0.5f * (XYSCALE[i] - 1))) * (INPUT_SIZE / gridWidth);
                                final float yPos = (y + (expit(out[0][y][x][b][1]) * XYSCALE[i]) - (0.5f * (XYSCALE[i] - 1))) * (INPUT_SIZE / gridWidth);

                                final float w = (float) (Math.exp(out[0][y][x][b][2]) * ANCHORS[2 * MASKS[i][b]]);
                                final float h = (float) (Math.exp(out[0][y][x][b][3]) * ANCHORS[2 * MASKS[i][b] + 1]);

                                float left = Math.max(0, xPos - w / 2);
                                float top = Math.max(0, yPos - h / 2);

                                final RectF rectF = new RectF(
                                        left,
                                        top,
                                        Math.min(cropSize.width -1, xPos + w / 2),
                                        Math.min(cropSize.height -1 , yPos + h / 2));
                                        
                                // Add detections
                                detections.add(new YoloV4Recognition("" + offset, objectName, confidenceInClass, rectF, detectedClass));
                            }
                        }
                    }
                }
                Log.i(TAG, "## Yolov4Classifier START!!!!! out[" + i + "] detect end!!");
            }
            // Process nms
            final ArrayList<YoloV4Recognition> recognitions = nms(detections);
            for (int i = 0; i < recognitions.size(); ++i) {
                YoloV4Recognition recognition = recognitions.get(i);
                RectF rect = recognition.getRect();
                String objectName = recognition.getObjectName();
                float confidence = recognition.getConfidence();

                int colorIndex = getColorFromMap(objectName);
                cropToFrameTransform.mapRect(rect);
                boxRecognitions.add(new BoxRecognition(rect, colorIndex));

                float[] labelPoint = {rect.left, rect.top};
                cropToFrameTransform.mapPoints(labelPoint);
                labelRecognitions.add(new LabelRecognition(objectName, confidence, labelPoint[0], labelPoint[1], colorIndex));

            }
        }
        return info;
    }
}
