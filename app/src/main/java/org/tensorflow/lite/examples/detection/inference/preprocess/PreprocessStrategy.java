package org.tensorflow.lite.examples.detection.inference.preprocess;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.enums.FRAME_CONTROL;
import org.tensorflow.lite.examples.detection.inference.InferenceData;

import java.io.ByteArrayOutputStream;

public abstract class PreprocessStrategy {

    public PreprocessStrategy() {}

    public Mat resizeKeepAspectRatio(Mat input, Size dstSize, Scalar bgcolor) {
        Mat output = new Mat();
        double h1 = dstSize.width * (input.rows() / (double) input.cols());
        double w2 = dstSize.height * (input.cols() / (double) input.rows());
        if (h1 <= dstSize.height) {
            Imgproc.resize(input, output, new Size(dstSize.width, h1));
        } else {
            Imgproc.resize(input, output, new Size(w2, dstSize.height));
        }
        int top = (int) (dstSize.height - output.rows()) / 2;
        int down = (int) (dstSize.height - output.rows() + 1) / 2;
        int left = (int) (dstSize.width - output.cols()) / 2;
        int right = (int) (dstSize.width - output.cols() + 1) / 2;
        Core.copyMakeBorder(output, output, top, down, left, right, Core.BORDER_CONSTANT, bgcolor);
        return output;
    }

    protected Bitmap buildBitmap(InferenceData data) {
        if (data.getModel().getFrameControl() == FRAME_CONTROL.PHOTOALBUM) {
            return data.getImage();
        } else {
            int width = data.getPreviewSize().getWidth();
            int height = data.getPreviewSize().getHeight();
            YuvImage yuv = new YuvImage(data.getData(), 17, width,
                    height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
            byte[] bytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

    public abstract InferenceData preprocess(InferenceData data);
}
