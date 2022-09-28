package org.tensorflow.lite.examples.detection.inference.preprocess;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.inference.InferenceData;

public class AixYoloPosenetPreprocessorStrategy extends PreprocessStrategy {
    @Override
    public InferenceData preprocess(InferenceData data) {
        Bitmap image = buildBitmap(data);
        Size cropSize = new Size(data.getModel().getCropSize().width,
                data.getModel().getCropSize().height);
        byte[] body = execute(image, cropSize, data.getOrientated(),
                data.getModel().getModelName());

        data.setData(body);
        return data;
    }

    public byte[] execute(Bitmap image, Size newSize, int oriented, MODEL_NAME name) {
        Mat argbImage = new Mat();
        Mat rgbImage = new Mat();

        Utils.bitmapToMat(image, argbImage);
        Imgproc.cvtColor(argbImage, rgbImage, Imgproc.COLOR_RGBA2RGB);
        //Mat resizeImage = resizeKeepAspectRatio(rgbImage, newSize, new Scalar(0,0,0));
        Mat resizeImage = new Mat();
        Imgproc.resize(rgbImage, resizeImage, newSize, 0, 0, Imgproc.INTER_LINEAR);

        Point point = new Point(newSize.width / 2, newSize.height / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(point, -oriented, 1.0);
        Imgproc.warpAffine(resizeImage, resizeImage, rotImage, resizeImage.size());

        // need to test for aix yolo
        resizeImage.convertTo(resizeImage, getCvType(name));

        byte[] return_bytes = new byte[
                (int) (resizeImage.width() * resizeImage.height() * resizeImage.channels())];
        resizeImage.get(0, 0, return_bytes);

        return return_bytes;
    }

    private int getCvType(MODEL_NAME modelName) {
        if (modelName.equals(MODEL_NAME.POSENET)) {
            return CvType.CV_8UC3; // aix posenet
        } else {
            return CvType.CV_8SC3; // aix yolov3
        }
    }
}
