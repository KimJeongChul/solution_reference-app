package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.util.Log;
import android.util.Pair;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.gsontypes.KeyPoint;
import org.tensorflow.lite.examples.detection.gsontypes.ValidPair;
import org.tensorflow.lite.examples.detection.drawtypes.CircleRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LineRecognition;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opencv.core.CvType.CV_8U;

public class LocalPosenetPostprocessStrategy extends PostprocessStrategy {

    private static final String TAG = LocalPosenetPostprocessStrategy.class.toString();

    public static final int[][] mapIdx = new int[][]{
            {31, 32}, {39, 40}, {33, 34}, {35, 36}, {41, 42}, {43, 44},
            {19, 20}, {21, 22}, {23, 24}, {25, 26}, {27, 28}, {29, 30},
            {47, 48}, {49, 50}, {53, 54}, {51, 52}, {55, 56}, {37, 38},
            {45, 46}
    };

    public static final int[][] posePairs = new int[][]{
            {1, 2}, {1, 5}, {2, 3}, {3, 4}, {5, 6}, {6, 7},
            {1, 8}, {8, 9}, {9, 10}, {1, 11}, {11, 12}, {12, 13},
            {1, 0}, {0, 14}, {14, 16}, {0, 15}, {15, 17}, {2, 17},
            {5, 16}
    };

    public InferInfo postprocess(InferInfo info) {
        if (info.hasResult()) {
            int nPoints = 18;
            List<Mat> netParts = (List<Mat>) info.getInferenceResult();
            List<List<KeyPoint>> detectedKeypoints = new ArrayList<>();
            List<KeyPoint> keyPointList = new ArrayList<>();
            List<CircleRecognition> circleRecognitions = new ArrayList<>();
            List<LineRecognition> lineRecognitions = new ArrayList<>();
            Map<DRAW_TYPE, Object> drawable = new HashMap<>();
            Matrix cropToFrameTransform = getTransform(info);

            for (int i = 0; i < nPoints; i++) {
                List<KeyPoint> keyPoints = getKeyPoints(netParts.get(i), 0.1);
                int keyPointId = 0;
                for (int j = 0; j < keyPoints.size(); j++, keyPointId++) {
                    keyPoints.get(j).setId(keyPointId);
                    keyPointList.add(keyPoints.get(j));
                }
                detectedKeypoints.add(keyPoints);
            }

            for (int i = 0; i < 18; i++) {
                for (int j = 0; j < detectedKeypoints.get(i).size(); j++) {
                    float x = (float) detectedKeypoints.get(i).get(j).getPoint().x;
                    float y = (float) detectedKeypoints.get(i).get(j).getPoint().y;
                    float[] point = {x, y};
                    cropToFrameTransform.mapPoints(point);
                    circleRecognitions.add(new CircleRecognition(point[0], point[1]));
                }
            }

            List<List<ValidPair>> validPairs = new ArrayList<>();
            Set<Integer> invalidPairs = new HashSet<>();
            getValidPairs(netParts, detectedKeypoints, validPairs, invalidPairs);

            List<int[]> personwiseKeypoints = getPersonwiseKeypoints(validPairs, invalidPairs);

            for (int i = 0; i < nPoints - 1; i++) {
                for (int n = 0; n < personwiseKeypoints.size(); n++) {
                    int indexA = personwiseKeypoints.get(n)[posePairs[i][0]];
                    int indexB = personwiseKeypoints.get(n)[posePairs[i][1]];

                    if (indexA == -1 || indexB == -1) {
                        continue;
                    }

                    KeyPoint kpA = detectedKeypoints.get(posePairs[i][0]).get(indexA);
                    KeyPoint kpB = detectedKeypoints.get(posePairs[i][1]).get(indexB);

                    float[] pointA = {(float) kpA.getPoint().x, (float) kpA.getPoint().y};
                    float[] pointB = {(float) kpB.getPoint().x, (float) kpB.getPoint().y};
                    cropToFrameTransform.mapPoints(pointA);
                    cropToFrameTransform.mapPoints(pointB);

                    lineRecognitions.add(
                            new LineRecognition(
                                    pointA[0],
                                    pointA[1],
                                    pointB[0],
                                    pointB[1])
                    );
                }
            }

            drawable.put(DRAW_TYPE.CIRCLE, circleRecognitions);
            drawable.put(DRAW_TYPE.LINE, lineRecognitions);
            info.setDrawable(drawable);
        }
        return info;
    }

    private List<int[]> getPersonwiseKeypoints(
            List<List<ValidPair>> validPairs, Set<Integer> invalidPairs) {
        List<int[]> personwiseKeypoints = new ArrayList<>();
        for (int k = 0; k < mapIdx.length; ++k) {
            if (invalidPairs.contains(k)) {
                continue;
            }

            List<ValidPair> localValidPairs = validPairs.get(k);

            int indexA = posePairs[k][0];
            int indexB = posePairs[k][1];

            for (int i = 0; i < localValidPairs.size(); i++) {
                boolean found = false;
                int personIdx = -1;

                for (int j = 0; !found && j < personwiseKeypoints.size(); j++) {
                    if (indexA < personwiseKeypoints.get(j).length &&
                            personwiseKeypoints.get(j)[indexA] == localValidPairs.get(i).getaId()) {
                        personIdx = j;
                        found = true;
                    }
                }/* j */

                if (found) {
                    int[] newPersonwisePoint = personwiseKeypoints.get(personIdx);
                    newPersonwisePoint[indexB] = localValidPairs.get(i).getbId();
                    personwiseKeypoints.set(personIdx, newPersonwisePoint);
                } else if (k < 17) {
                    int[] lpkp = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
                    lpkp[indexA] = localValidPairs.get(i).getaId();
                    lpkp[indexB] = localValidPairs.get(i).getbId();
                    personwiseKeypoints.add(lpkp);
                }
            }/* i */
        }/* k */

        return personwiseKeypoints;
    }

    private List<KeyPoint> getKeyPoints(Mat probMap, double threshold) {
        List<KeyPoint> keyPoints = new ArrayList<>();
        Mat smoothProbMap = new Mat();
        Imgproc.GaussianBlur(probMap, smoothProbMap, new org.opencv.core.Size(3, 3), 0, 0);

        Mat maskedProbMap = new Mat();
        Imgproc.threshold(smoothProbMap, maskedProbMap, threshold, 255, Imgproc.THRESH_BINARY);

        maskedProbMap.convertTo(maskedProbMap, CV_8U, 1);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(maskedProbMap, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++) {
            Mat blobMask = Mat.zeros(smoothProbMap.rows(), smoothProbMap.cols(), smoothProbMap.type());
            Scalar color = new Scalar(1);
            Imgproc.fillConvexPoly(blobMask, contours.get(i), color);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(smoothProbMap.mul(blobMask));
            double maxVal = mmr.maxVal;
            Point maxLoc = mmr.maxLoc;

            KeyPoint keyPoint = new KeyPoint(maxLoc, (float) maxVal);
            keyPoints.add(keyPoint);
        }
        return keyPoints;
    }

    private void getValidPairs(List<Mat> netOutputParts, List<List<KeyPoint>> detectedKeyPoints,
                               List<List<ValidPair>> validPairs, Set<Integer> invalidPairs) {
        int nInterpSamples = 10;
        float pafScoreTh = 0.1f;
        float confTh = 0.7f;

        for (int k = 0; k < mapIdx.length; k++) {
            Mat pafA = netOutputParts.get(mapIdx[k][0]);
            Mat pafB = netOutputParts.get(mapIdx[k][1]);

            List<KeyPoint> candA = detectedKeyPoints.get(posePairs[k][0]);//list of part1
            List<KeyPoint> candB = detectedKeyPoints.get(posePairs[k][1]);//list of part2

            int nA = candA.size();
            int nB = candB.size();

            if (nA != 0 && nB != 0) {
                List<ValidPair> localValidPairs = new ArrayList<>();

                for (int i = 0; i < nA; i++) {
                    int maxJ = -1;
                    float maxScore = -1;
                    boolean found = false;

                    for (int j = 0; j < nB; j++) {
                        double xDistance = candB.get(j).getPoint().x - candA.get(i).getPoint().x;
                        double yDistance = candB.get(j).getPoint().y - candA.get(i).getPoint().y;
                        double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance);
                        if (distance == 0) {
                            continue;
                        }

                        xDistance /= distance;
                        yDistance /= distance;

                        List<Point> interpCoords =
                                populateInterpPoints(candA.get(i).getPoint(),
                                        candB.get(j).getPoint(), nInterpSamples);

                        List<Pair<Double, Double>> pafInterp = new ArrayList<>();
                        for (int l = 0; l < interpCoords.size(); l++) {
                            pafInterp.add(
                                    new Pair<Double, Double>(
                                            pafA.get((int) interpCoords.get(l).y,
                                                    (int) interpCoords.get(l).x)[0],
                                            pafB.get((int) interpCoords.get(l).y,
                                                    (int) interpCoords.get(l).x)[0]
                                    )
                            );
                        }
                        List<Float> pafScores = new ArrayList<>();
                        float sumOfPafScores = 0;
                        int numOverTh = 0;
                        for (int l = 0; l < pafInterp.size(); l++) {
                            float score = (float) (pafInterp.get(l).first * xDistance +
                                    pafInterp.get(l).second * yDistance);
                            sumOfPafScores += score;
                            if (score > pafScoreTh) {
                                numOverTh++;
                            }
                            pafScores.add(score);
                        }

                        float avgPafScore = sumOfPafScores / ((float) pafInterp.size());

                        if ((float) numOverTh / (float) nInterpSamples > confTh) {
                            if (avgPafScore > maxScore) {
                                maxJ = j;
                                maxScore = avgPafScore;
                                found = true;
                            }
                        }
                    } /* j */
                    if (found) {
                        localValidPairs.add(new ValidPair(candA.get(i).getId(),
                                candB.get(maxJ).getId(), maxScore));
                    }
                }/* i */
                validPairs.add(localValidPairs);
            } else {
                invalidPairs.add(k);
                validPairs.add(new ArrayList<ValidPair>());
            }
        }/* k */
    }

    private List<Point> populateInterpPoints(Point a, Point b, int numPoints) {
        List<Point> interpCoords = new ArrayList<>();
        float xStep = ((float) (b.x - a.x)) / (float) (numPoints - 1);
        float yStep = ((float) (b.y - a.y)) / (float) (numPoints - 1);

        interpCoords.add(a);

        for (int i = 1; i < numPoints - 1; ++i) {
            interpCoords.add(new Point(a.x + xStep * i, a.y + yStep * i));
        }

        interpCoords.add(b);
        return interpCoords;
    }
}
