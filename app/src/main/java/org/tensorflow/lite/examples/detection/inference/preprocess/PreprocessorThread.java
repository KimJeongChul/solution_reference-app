package org.tensorflow.lite.examples.detection.inference.preprocess;

import android.util.Log;

import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.queuehandler.QueueHandler;

public class PreprocessorThread extends Thread {

  private static String TAG = "PreProcessor";

  private boolean stopped;
  private QueueHandler queueHandler;

  public PreprocessorThread(QueueHandler queueHandler) {
    super("preprocessorThread");
    this.queueHandler = queueHandler;
  }

  public void quitThreadSafely() {
    this.stopped = true;
  }

  @Override
  public void run() {
    while (!stopped) {
      InferenceData data = queueHandler.getData();
      if (data != null) {
        PreprocessStrategy strategy = PreprocessStrategyFactory.getPreprocessor(data);
        Log.i(TAG, String.format("## preprocess strategy:%s", strategy));
        InferenceData preprocessed = strategy.preprocess(data);
        queueHandler.inference(preprocessed);
      }
    }
  }
}
