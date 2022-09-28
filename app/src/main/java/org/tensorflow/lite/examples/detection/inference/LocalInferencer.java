package org.tensorflow.lite.examples.detection.inference;

import android.content.Context;
import android.util.Log;

import org.opencv.core.CvException;
import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;
import org.tensorflow.lite.examples.detection.inference.local.LocalInferenceModel;
import org.tensorflow.lite.examples.detection.inference.local.LocalPosenetModel;
import org.tensorflow.lite.examples.detection.inference.local.LocalSegmentModel;
import org.tensorflow.lite.examples.detection.inference.local.LocalYolov3Model;

import java.util.HashMap;
import java.util.Map;

public class LocalInferencer implements InferenceStrategy {

    private static final String TAG = LocalInferencer.class.toString();

    private final Context context;
    private LocalInferenceModel modelStrategy;
    private ModelInfo modelInfo;

    private final Map<MODEL_NAME, LocalInferenceModel> localModels;

    public LocalInferencer(Context context, ModelInfo modelInfo) {
        this.context = context;
        this.modelInfo = modelInfo;
        this.localModels = new HashMap<>();
        initialize();
    }

    private void initialize() {
        try {
            Log.i(TAG, "## initialize local posenet model");
            LocalPosenetModel localPosenetModel = new LocalPosenetModel(context);
            localModels.put(MODEL_NAME.POSENET, localPosenetModel);
        } catch (CvException e) {
            Log.e(TAG, "## local posenet initialization failed " + e);
        }

        try {
            Log.i(TAG, "## initialize local yolo model");
            LocalYolov3Model localYoloModel = new LocalYolov3Model(context);
            localModels.put(MODEL_NAME.YOLOV2, localYoloModel);
            localModels.put(MODEL_NAME.YOLOV3, localYoloModel);
        } catch (CvException e) {
            Log.e(TAG, "## local yolo initialize failed " + e);
        }

//        try {
//            Log.i(TAG, "## initialize local segment model");
//            LocalSegmentModel localSegmentModel = new LocalSegmentModel(context);
//            localModels.put(MODEL_NAME.SEGMENTATION, localSegmentModel);
//        } catch (CvException e) {
//            Log.e(TAG, "## local segmenation initialization failed " + e);
//        }
        setModel(this.modelInfo);
    }

    @Override
    public InferInfo inference(InferenceData data) {
        return modelStrategy.inference(data.getData());
    }

    @Override
    public void setModel(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
        this.modelStrategy = localModels.get(modelInfo.getModelName());
    }

    @Override
    public void stopInference() {
    }

    @Override
    public void startInference() {
    }
}

