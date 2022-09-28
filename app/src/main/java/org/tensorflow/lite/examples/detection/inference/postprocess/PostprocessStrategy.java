package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.Matrix;
import android.util.Log;

import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Size;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public abstract class PostprocessStrategy {
    private static String TAG = PostprocessStrategy.class.toString();

    private Map<String, Integer> colorCode;

    public PostprocessStrategy() {
        this.colorCode = new HashMap<>();
        this.colorCode.put("person", 0);
    }

    protected int getColorFromMap(String objectName) {
        int index = 0;
        if (colorCode.containsKey(objectName)) {
            index = colorCode.get(objectName);
        } else {
            index = maxUsingCollectionsMax(colorCode) + 1;
            colorCode.put(objectName, index);
        }
        return index;
    }

    protected <K, V extends Comparable<V>> V maxUsingCollectionsMax(Map<K, V> map) {
        Map.Entry<K, V> maxEntry = Collections.max(map.entrySet(), new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                return e1.getValue()
                        .compareTo(e2.getValue());
            }
        });
        return maxEntry.getValue();
    }

    protected Matrix getTransform(InferInfo info) {
        Size cropSize = info.getModelInfo().getCropSize();
        int previewWidth = info.getPreviewSize().getWidth();
        int previewHeight = info.getPreviewSize().getHeight();
        Log.i(TAG, String.format("## crop width:%s, height:%s, preview width:%s height:%s",
                cropSize.width, cropSize.height, previewWidth, previewHeight));
        Matrix cropToFrameTransform = ImageUtils.getTransformationMatrix(cropSize.width, cropSize.height,
                previewWidth, previewHeight, 0, false);
        return cropToFrameTransform;
    }

    public int byteToInt(byte bytes[]) {
        return ((((int) bytes[0] & 0xff)) |
                (((int) bytes[1] & 0xff) << 8) |
                (((int) bytes[2] & 0xff) << 16) |
                (((int) bytes[3] & 0xff) << 24));
    }

    public float byteToFloat(byte bytes[]) {
        int value = byteToInt(bytes);
        return Float.intBitsToFloat(value);
    }

    public int[] byteArrayToIntArray(byte bytes[]) {
        int[] iArr = new int[bytes.length / 4];
        int offset = 0;

        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = byteToInt(Arrays.copyOfRange(bytes, offset, offset + 4));
            offset += 4;
        }
        return iArr;
    }

    public float[] byteArrayToFloatArray(byte bytes[]) {
        float[] fArr = new float[bytes.length / 4];
        int offset = 0;

        for (int i = 0; i < fArr.length; i++) {
            fArr[i] = byteToFloat(Arrays.copyOfRange(bytes, offset, offset + 4));
            offset += 4;
        }
        return fArr;
    }

    public abstract InferInfo postprocess(InferInfo info);
}
