package org.tensorflow.lite.examples.detection.inference.preprocess;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Size;
import org.tensorflow.lite.examples.detection.inference.InferenceData;

import java.io.ByteArrayOutputStream;

public class LocalGPUPreprocessorStrategy extends PreprocessStrategy {
    @Override
    public InferenceData preprocess(InferenceData data) {
        Bitmap image = buildBitmap(data);
        Size previewSize = data.getPreviewSize();
        Size cropSize = data.getModel().getCropSize();
        byte[] body = preprocessForGpuAndLocal(image, previewSize.getWidth(),
                previewSize.getHeight(), cropSize.width,
                cropSize.height, data.getOrientated());
        data.setData(body);
        return data;
    }

    private byte[] preprocessForGpuAndLocal(Bitmap bitmap, int previewWidth, int previewHeight,
                                            int cropWidth, int cropHeight, int oriented) {

        Matrix frameToCropTransform =
                ImageUtils.getTransformationMatrix(previewWidth, previewHeight, cropWidth, cropHeight,
                        oriented, false);
        Bitmap croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight,
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(bitmap, frameToCropTransform, null);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}
