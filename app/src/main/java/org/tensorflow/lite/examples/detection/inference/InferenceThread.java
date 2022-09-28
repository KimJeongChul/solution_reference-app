package org.tensorflow.lite.examples.detection.inference;

import android.util.Log;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.queuehandler.QueueHandler;

public class InferenceThread extends Thread {

    private static String TAG = InferenceThread.class.toString();

    private InferenceStrategy strategy;
    private QueueHandler queueHandler;

    boolean stopped;

    public InferenceThread(InferenceStrategy strategy, QueueHandler queueHandler) {
        this.strategy = strategy;
        this.queueHandler = queueHandler;
    }

    public void quitThreadSafely() {
        this.stopped = true;
    }

    @Override
    public void run() {
        strategy.startInference();
        while (!stopped) {
            InferenceData preprocessed = queueHandler.getPreprocessed();
            if (preprocessed != null) {
                try {
                    long startTime = System.currentTimeMillis();
                    InferInfo result = strategy.inference(preprocessed);
                    result.setModelInfo(preprocessed.getModel());
                    result.setPreviewSize(preprocessed.getPreviewSize());
                    long totalTime = System.currentTimeMillis() - startTime;
                    result.setTotalTime(totalTime);
                    queueHandler.draw(result);
                    // this is need for local inferencer
                    // thread should be stopped until postprocessing finished
                    // or post process will failed, the reason is unknown
                    queueHandler.waiting();
                } catch (Exception e) {
                    Log.e(TAG, "inference error", e);
                }
            }
        }
    }

}
