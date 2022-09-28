package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.dnn.Dnn;
import org.opencv.utils.Converters;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.drawtypes.BoxRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LabelRecognition;
import org.tensorflow.lite.examples.detection.env.Size;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.local.LocalYolov3Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalYoloPostprocessStrategy extends PostprocessStrategy {

    private static final String TAG = LocalYoloPostprocessStrategy.class.toString();
    private float threshold = 0.5f;

    public InferInfo postprocess(InferInfo info) {
        if (info.hasResult()) {
            Size cropSize = info.getModelInfo().getCropSize();
            Matrix cropToFrameTransform = getTransform(info);
            List<Mat> result = (List<Mat>) info.getInferenceResult();
            List<BoxRecognition> boxRecognitions = new ArrayList<>();
            List<LabelRecognition> labelRecognitions = new ArrayList<>();
            Map<DRAW_TYPE, Object> drawable = new HashMap<>();
            Mat frame = info.getFrame();
            List<Integer> clsIds = new ArrayList<>(); //class id list
            List<Float> confs = new ArrayList<>(); //confidence list
            List<Rect2d> rects = new ArrayList<>(); //rect list

            if (result.size() < 30) {
                for (int i = 0; i < result.size(); i++) {//big yolo - 3
                    Mat level = result.get(i);

                    for (int j = 0; j < level.rows(); j++) {
                        Mat row = level.row(j);//detection candidates
                        Mat scores = row.colRange(5, level.cols());
                        // 0,1,2,3 = center_x, center_y, width, height

                        Core.MinMaxLocResult mm = Core.minMaxLoc(scores);

                        float confidence = (float) mm.maxVal;
                        Point classIdPoint = mm.maxLoc;

                        if (confidence > threshold) {
                            int centerX = (int) (row.get(0, 0)[0] * frame.cols());
                            int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                            int width = (int) (row.get(0, 2)[0] * frame.cols());
                            int height = (int) (row.get(0, 3)[0] * frame.rows());

                            int left = centerX - width / 2;
                            int top = centerY - height / 2;

                            clsIds.add((int) classIdPoint.x);
                            confs.add(confidence);
                            rects.add(new Rect2d(left, top, width, height));
                        }
                    }
                }
            }

            if (confs.size() >= 1) {
                float nmsThresh = 0.5f;
                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
                Rect2d[] boxesArray = rects.toArray(new Rect2d[0]);
                MatOfRect2d boxes = new MatOfRect2d(boxesArray);
                MatOfInt indices = new MatOfInt();
                Dnn.NMSBoxes(boxes, confidences, threshold, nmsThresh, indices);


                //draw result boxes
                int[] ind = indices.toArray();
                if (ind.length < 30) {
                    for (int i = 0; i < ind.length; ++i) {

                        int idx = ind[i];
                        Rect2d box = boxesArray[idx];
                        int idGuy = clsIds.get(idx);
                        float conf = confs.get(idx);

                        Point minPoint = box.tl();
                        Point maxPoint = box.br();

                        float left = (float) minPoint.x;
                        float top = (float) minPoint.y;
                        float right = (float) maxPoint.x;
                        float bottom = (float) maxPoint.y;

                        top = Math.max(0, (float) Math.floor(top + 0.5));
                        left = Math.max(0, (float) Math.floor(left + 0.5));
                        bottom = Math.min(cropSize.height, (float) Math.floor(bottom + 0.5));
                        right = Math.min(cropSize.width, (float) Math.floor(right + 0.5));

                        RectF rectF = new RectF(left, top, right, bottom);
                        cropToFrameTransform.mapRect(rectF);
                        String objectName = LocalYolov3Model.cocoNames.get(idGuy);

                        int index = getColorFromMap(objectName);
                        boxRecognitions.add(new BoxRecognition(rectF, index));
                        float[] labelPoint = {left, top};
                        cropToFrameTransform.mapPoints(labelPoint);
                        labelRecognitions.add(new LabelRecognition(objectName, conf, labelPoint[0],
                                labelPoint[1], index));
                    }
                }
            }
            drawable.put(DRAW_TYPE.RECT, boxRecognitions);
            drawable.put(DRAW_TYPE.LABEL, labelRecognitions);
            info.setDrawable(drawable);
        }
        return info;
    }
}
