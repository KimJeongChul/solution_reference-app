package org.tensorflow.lite.examples.detection.inference.postprocess;

import android.graphics.PostProcessor;
import android.util.Log;

import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.InferenceStrategy;
import org.tensorflow.lite.examples.detection.queuehandler.QueueHandler;

public class PostprocessThread extends Thread {

    private String TAG = PostprocessThread.class.toString();
    private boolean stopped;
    private QueueHandler queueHandler;

    private InferenceStrategy inferenceMode;
    private PostprocessHandler handler;

    public PostprocessThread(QueueHandler queueHandler, PostprocessHandler handler) {
        this.queueHandler = queueHandler;
        this.handler = handler;
    }

    public interface PostprocessHandler {
        public void handleInferInfo(InferInfo info);
    }

    public void quitThreadSafely() {
        this.stopped = true;
    }

    @Override
    public synchronized void run() {
        while (!stopped) {
            InferInfo info = queueHandler.getInferenced();
            if (info != null) {
                try {
                    PostprocessStrategy postprocessStrategy =
                            PostprocessStrategyFactory.getPostprocessor(info.getModelInfo());
                    Log.i(TAG, String.format("## postprocess:%s",
                            postprocessStrategy.getClass()));
                    InferInfo postprocessed = postprocessStrategy.postprocess(info);
                    this.handler.handleInferInfo(postprocessed);
                    this.queueHandler.wake();
                } catch (Exception e) {
                    Log.e(TAG, "Fail to postprocess", e);
                }
            }
        }
    }
}
