package org.tensorflow.lite.examples.detection.queuehandler;

import android.util.Log;

import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class LocalQueueHandler extends RemoteQueueHandler {
  public String TAG = "LocalQueueHandler";

  private BlockingQueue<InferInfo> post_inference_queue;
  private Object syncToken = new Object();

  public LocalQueueHandler(BlockingQueue preprocess_queue,
                           BlockingQueue inference_queue, BlockingQueue post_inference_queue) {
    super(preprocess_queue, inference_queue, post_inference_queue);
    this.post_inference_queue = post_inference_queue;
  }

  @Override
  public void draw(InferInfo inferencedObject) {
    post_inference_queue.offer(inferencedObject);
  }

  @Override
  public void waiting() {
    synchronized (syncToken) {
      try {
        syncToken.wait();
      } catch (Exception e) {
        Log.i(TAG, "fail to wait in local queue handler, may be interrupted");
      }
    }
  }

  @Override
  public void wake() {
    synchronized (syncToken) {
      syncToken.notify();
    }
  }
}
