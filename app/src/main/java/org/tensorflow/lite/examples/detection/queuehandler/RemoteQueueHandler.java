package org.tensorflow.lite.examples.detection.queuehandler;

import android.util.Log;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.InferenceData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class RemoteQueueHandler implements QueueHandler {

  public String TAG = "QueueHandler";

  private BlockingQueue<InferenceData> preprocess_queue;
  private BlockingQueue<InferenceData> inference_queue;
  private BlockingQueue<InferInfo> post_inference_queue;

  public RemoteQueueHandler(BlockingQueue preprocess_queue,
                            BlockingQueue inference_queue, BlockingQueue post_inference_queue) {
    this.preprocess_queue = preprocess_queue;
    this.inference_queue = inference_queue;
    this.post_inference_queue = post_inference_queue;
  }

  @Override
  public void feed(InferenceData data) {
    preprocess_queue.offer(data);
  }

  @Override
  public InferenceData getData() {
    try {
      return preprocess_queue.take();
    } catch (InterruptedException e) {
      Log.i(TAG, "camera thread inturrupted");
      return null;
    }
  }

  @Override
  public void inference(InferenceData preprocessed) {
    inference_queue.offer(preprocessed);
  }

  @Override
  public InferenceData getPreprocessed() {
    try {
      return inference_queue.take();
    } catch (InterruptedException e) {
      Log.i(TAG, "preprocess thrad inturrupted");
      return null;
    }
  }

  @Override
  public void draw(InferInfo info) {
    post_inference_queue.offer(info);
  }

  @Override
  public InferInfo getInferenced() {
    try {
      return post_inference_queue.take();
    } catch (InterruptedException e) {
      Log.i(TAG, "inference thrad inturrupted");
      return null;
    }
  }

  @Override
  public void clear() {
    preprocess_queue.clear();
    inference_queue.clear();
    post_inference_queue.clear();
  }

  @Override
  public void waiting() {
  }

  @Override
  public void wake() {
  }
}
