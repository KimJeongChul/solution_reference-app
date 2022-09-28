package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.inference.postprocess.PostprocessThread;
import org.tensorflow.lite.examples.detection.enums.MODE;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.InferenceStrategy;
import org.tensorflow.lite.examples.detection.inference.InferenceThread;
import org.tensorflow.lite.examples.detection.inference.LocalInferencer;
import org.tensorflow.lite.examples.detection.inference.RemoteInferencer;
import org.tensorflow.lite.examples.detection.inference.preprocess.PreprocessorThread;
import org.tensorflow.lite.examples.detection.queuehandler.LocalQueueHandler;
import org.tensorflow.lite.examples.detection.queuehandler.QueueHandler;
import org.tensorflow.lite.examples.detection.queuehandler.RemoteQueueHandler;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;
import org.tensorflow.lite.examples.detection.settings.ModelSetting;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class InferenceFlowManager {

  private static String TAG = InferenceFlowManager.class.toString();
  private Context context;

  /* strategies */
  private RemoteInferencer mecInferencer;
  private LocalInferencer localInferencer;

  /* mode setting */
  private InferenceStrategy inferenceStrategy;

  /* sync or async */
  private QueueHandler queueHandler;
  private LocalQueueHandler localQueueHandler;
  private RemoteQueueHandler remoteQueueHandler;

  /* threads */
  PreprocessorThread preprocessorThread;
  InferenceThread inferenceThread;
  PostprocessThread postprocessThread;

  // current status
  private InferenceFlowHandler handler;
  private ModelSetting setting;
  private MODE mode;

  /* queues */
  public BlockingQueue<byte[]> preprocess_queue = new LinkedBlockingDeque<>(1);
  public BlockingQueue<byte[]> inference_queue = new LinkedBlockingQueue<>(1);
  public BlockingQueue<Object> post_inference_queue = new LinkedBlockingDeque<>(1);

  public InferenceFlowManager(Context context, InferenceFlowHandler handler) {
    this.context = context;
    this.handler = handler;
    ModelInfo.initialize();
  }

  public interface InferenceFlowHandler {
    public void handleInfeInfo(InferInfo info);
  }

  public void feed(InferenceData data) {
    queueHandler.feed(data);
  }

  public void setMode(MODE mode) {
    Log.i(TAG, String.format("## try to change inferece mode from:%s to %s", this.mode, mode));
    if (mode != this.mode) {
      stop();
      start(setting, mode);
    }
  }

  public void start(ModelSetting modelSetting, MODE mode) {
    this.mode = mode;
    setting = modelSetting;
    Log.i(TAG, String.format("## start inference flow mode:%s", mode));
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this.context) {
      @Override
      public void onManagerConnected(int status) {
        super.onManagerConnected(status);
        if (status == LoaderCallbackInterface.SUCCESS) {
          Log.i(TAG, "## opencv loaded successfully");
          init();
        } else {
          Log.e(TAG, "## fail to load opencv");
        }
      }
    };

    if (!OpenCVLoader.initDebug()) {
      Log.d(TAG, "## try to load opencv");
      OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this.context, mLoaderCallback);
    } else {
      Log.d(TAG, "## opencv already loaded");
      mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }
  }

  public void stop() {
    Log.i(TAG, "## stop inference flow");
    if (preprocessorThread != null) {
      preprocessorThread.quitThreadSafely();
      preprocessorThread.interrupt();
      preprocessorThread = null;
    }

    if (inferenceThread != null) {
      inferenceThread.quitThreadSafely();
      inferenceThread.interrupt();
      inferenceThread = null;
    }

    if (postprocessThread != null) {
      postprocessThread.quitThreadSafely();
      postprocessThread.interrupt();
      postprocessThread = null;
    }
  }

  private void init() {
    Log.i(TAG, "## init inference flow!!");

    ModelInfo modelInfo = ModelInfo.getModelInfo(setting, mode);
    mecInferencer = new RemoteInferencer(modelInfo);
    if (localInferencer == null) {
      localInferencer = new LocalInferencer(this.context, modelInfo);
    }
    localInferencer.setModel(modelInfo);

    localQueueHandler = new LocalQueueHandler(preprocess_queue, inference_queue, post_inference_queue);
    remoteQueueHandler = new RemoteQueueHandler(preprocess_queue, inference_queue, post_inference_queue);

    queueHandler = mode == MODE.MEC ? remoteQueueHandler : localQueueHandler;
    inferenceStrategy = mode == MODE.MEC ? mecInferencer : localInferencer;

    Log.i(TAG, String.format("## init inferene flow model name:%s, device type:%s, mode:%s",
            modelInfo.getModelName(), modelInfo.getDeviceType(), mode));
    preprocessorThread = new PreprocessorThread(queueHandler);
    inferenceThread = new InferenceThread(inferenceStrategy, queueHandler);
    postprocessThread = new PostprocessThread(queueHandler, new PostprocessThread.PostprocessHandler() {
      @Override
      public void handleInferInfo(InferInfo info) {
        if (handler != null) {
          handler.handleInfeInfo(info);
        }
      }
    });

    preprocessorThread.start();
    inferenceThread.start();
    postprocessThread.start();
  }
}
