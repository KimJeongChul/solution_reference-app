package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.graphics.RectF;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.drawtypes.BoxRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LabelRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.MaskRecognition;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.CvType.CV_8U;

public class LocalSegmentPostprocessStrategy extends PostprocessStrategy {
    public static List<String> segmentCocos = Arrays.asList(
            "person", "bicycle", "car", "motorbike", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "", "stop sign", "parking meter", "bench", "bird",
            "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "",
            "backpack", "umbrella", "", "", "handbag", "tie", "suitcase", "frisbee", "skis",
            "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard",
            "surfboard", "tennis racket", "bottle", "", "wine glass", "cup", "fork", "knife",
            "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli", "carrot",
            "hot dog", "pizza", "doughnut", "cake", "chair", "sofa", "potted plant", "bed",
            "", "dining table", "", "", "toilet", "", "tv monitor", "laptop", "computer mouse",
            "remote control", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
            "refrigerator", "", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
            "toothbrush");

    @Override
    public InferInfo postprocess(InferInfo info) {
        if (info.hasResult()) {
            Matrix cropToFrameTransform = getTransform(info);
            List<MaskRecognition> maskRecognitions = new ArrayList<>();
            List<BoxRecognition> boxRecognitions = new ArrayList<>();
            List<LabelRecognition> labelRecognitions = new ArrayList<>();
            List<String> cocoNames = segmentCocos;
            Map<DRAW_TYPE, Object> drawable = new HashMap<>();

            Mat frame = info.getFrame();

            List<Mat> outs = (List<Mat>) info.getInferenceResult();
            Mat outDetections = outs.get(0);
            Mat outMasks = outs.get(1);
            float confThreshold = 0.5f;

            // Output size of masks is NxCxHxW where
            // N - number of detected boxes
            // C - number of classes (excluding background)
            // HxW - segmentation shape
            int numDetections = outDetections.size(2);
            int topDetection = outMasks.size(0);
            int numClasses = outMasks.size(1);
            int outMaskHeight = outMasks.size(2);
            int outMaskWidth = outMasks.size(3);

            outDetections = outDetections.reshape(1, (int) outDetections.total() / 7);
            outMasks = outMasks.reshape(1, outMaskHeight * topDetection * numClasses); //column은 알아서 계산됨

            for (int i = 0; i < numDetections; i++) {
                float score = (float) outDetections.get(i, 2)[0];

                if (score > confThreshold) {
                    // Extract the bounding box
                    int classId = (int) outDetections.get(i, 1)[0];
                    int left = (int) (frame.cols() * outDetections.get(i, 3)[0]);
                    int top = (int) (frame.rows() * outDetections.get(i, 4)[0]);
                    int right = (int) (frame.cols() * outDetections.get(i, 5)[0]);
                    int bottom = (int) (frame.rows() * outDetections.get(i, 6)[0]);

                    left = Math.max(0, Math.min(left, frame.cols() - 1));
                    top = Math.max(0, Math.min(top, frame.rows() - 1));
                    right = Math.max(0, Math.min(right, frame.cols() - 1));
                    bottom = Math.max(0, Math.min(bottom, frame.rows() - 1));
                    RectF rectF = new RectF(left, top, right, bottom);
                    int boxWidth = right - left + 1;
                    int boxHeight = bottom - top + 1;

                    float[] labelPoint = {left, top};
                    cropToFrameTransform.mapPoints(labelPoint);
                    cropToFrameTransform.mapRect(rectF);
                    boxRecognitions.add(new BoxRecognition(rectF, 0));
                    labelRecognitions.add(new LabelRecognition(cocoNames.get(classId), score, labelPoint[0], labelPoint[1], 0));

                    Mat detectionPart = outMasks.submat(outMaskHeight * numClasses * i, outMaskHeight * numClasses * (i + 1), 0, outMaskHeight);
                    Mat objectMask = detectionPart.submat(outMaskHeight * classId, outMaskHeight * (classId + 1), 0, outMaskHeight);

                    Imgproc.resize(objectMask, objectMask, new Size(boxWidth, boxHeight));
                    Imgproc.threshold(objectMask, objectMask, 0.3, 1, Imgproc.THRESH_BINARY);
                    objectMask.convertTo(objectMask, CV_8U);

                    List<MatOfPoint> contours = new ArrayList<>();
                    Mat hierarchy = new Mat();

                    Imgproc.findContours(objectMask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
                    List<Point[]> contourPoints = new ArrayList<>();

                    for (MatOfPoint matPoints : contours) {
                        Point[] pointsPerContour = matPoints.toArray();
                        for (int k = 0; k < pointsPerContour.length; k++) {
                            float[] kth_point = {(float) pointsPerContour[k].x + left,
                                    (float) pointsPerContour[k].y + top};
                            cropToFrameTransform.mapPoints(kth_point);
                            Point fixedPoint = new Point();
                            fixedPoint.x = kth_point[0];//+labelPoint[0];
                            fixedPoint.y = kth_point[1];//+labelPoint[1];
                            pointsPerContour[k] = fixedPoint;
                        }
                        contourPoints.add(pointsPerContour);
                    }

                    String objectName = cocoNames.get(classId);
                    int index = getColorFromMap(objectName);
                    maskRecognitions.add(new MaskRecognition(contourPoints, index));
                }
            }
            drawable.put(DRAW_TYPE.RECT, boxRecognitions);
            drawable.put(DRAW_TYPE.LABEL, labelRecognitions);
            drawable.put(DRAW_TYPE.MASK, maskRecognitions);
            info.setDrawable(drawable);
        }
        return info;
    }
}
