package org.tensorflow.lite.examples.detection.inference.local;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public abstract class LocalInferenceModel {

    private static String TAG = LocalInferenceModel.class.toString();

    private Context context;
    private AssetManager assetManager;
    protected int cropSize = 608;
    protected Mat frame;

    public LocalInferenceModel(Context context) {
        this.context = context;
        this.assetManager = context.getResources().getAssets();
    }

    public Mat preprocess(byte[] bytes, Size size, int colorFormat, float scalefactor) {
        this.frame = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
        Imgproc.cvtColor(frame, frame, colorFormat);
        Mat imageBlob = Dnn.blobFromImage(frame, scalefactor, size, new Scalar(0, 0, 0),
                false, false);
        return imageBlob;
    }

    public String decompress(String fileName) {
        String path = context.getCacheDir() + "/" + fileName;
        File file = new File(path);
        try {
            InputStream inputStream = assetManager.open(fileName);

            int sizeOfStream = inputStream.available();
            byte[] buffer = new byte[sizeOfStream];
            inputStream.read(buffer);// Temporarily read into the buffer.
            inputStream.close();

            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(buffer);// Transfer the temporarily read buffer to the file.
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        String absoluteFilePath = file.getAbsolutePath();
        return absoluteFilePath;
    }

    public abstract InferInfo inference(byte[] bytes);
}
