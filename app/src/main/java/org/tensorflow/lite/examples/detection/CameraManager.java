package org.tensorflow.lite.examples.detection;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("ALL")
public class CameraManager {

  private static String TAG = "Camera";
  private SurfaceView view;
  private Camera camera;
  private CameraHandler handler;

  public CameraManager(SurfaceView view, CameraHandler handler) {
    this.view = view;
    this.view.getHolder().addCallback(listener);
    this.handler = handler;
  }

  public interface CameraHandler {
    public void handleBytes(byte[] bytes);

    public void handleCameraChanged(Size previewSize, Size canvasSize);

    public void handleCameraDisposed();
  }

  public void playCamera() {
    if (camera != null) {
      camera.startPreview();
    }
  }

  public void stopCamera() {
    Log.i(TAG, "## stop camera");
    if (camera != null) {
      camera.stopPreview();
    }
  }

  private void openCamera() {
    Log.i(TAG, "## open camera");
    if (camera == null) {
      int index = getCameraId();
      Log.i(TAG, "## surface created so open camera id:" + index);
      camera = android.hardware.Camera.open(index);
    }
  }

  private void closeCamera() {
    Log.i(TAG, "## close camera.");
    if (camera != null) {
      try {
        camera.stopPreview();
        //camera.release();
        handler.handleCameraDisposed();
      } catch (Exception e) {
        Log.e(TAG, "## fail to handle camera dispose", e);
      } finally {
        camera = null;
      }
    }
  }

  private final SurfaceHolder.Callback listener = new SurfaceHolder.Callback() {
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
      Log.i(TAG, "## surface created");
      openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
      Log.i(TAG, String.format("## surface changed camera width:%s, height:%s", width, height));
      openCamera();
      android.hardware.Camera.Parameters parameters = camera.getParameters();
      int format = parameters.getPreviewFormat();
      Log.i(TAG, String.format("## camera preview format:%s", format));
      List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
      Size[] sizes = new Size[cameraSizes.size()];
      int k = 0;
      for (Camera.Size size : cameraSizes) {
        Log.i(TAG, String.format("## support preview width:%s, height:%s", size.width, size.height));
        sizes[k++] = new Size(size.width, size.height);
      }
      Size previewSize =
              chooseOptimalSize(sizes, width, height);
      int previewWidth = previewSize.getWidth();
      int previewHeight = previewSize.getHeight();
      Log.i(TAG, String.format("## final preview width:%s, height:%s", previewWidth,
              previewHeight));

      parameters.setPreviewSize(previewWidth, previewHeight);
      try {
        camera.setPreviewDisplay(surfaceHolder);
        camera.setDisplayOrientation(90);
      } catch (IOException e) {
        Log.e(TAG, "Fail to open camera", e);
      }
      camera.setParameters(parameters);
      camera.setPreviewCallback(new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera current) {
          if (camera != null) {
            try {
              handler.handleBytes(bytes);
            } catch (Exception e) {
              Log.e(TAG, "## fail to handle camera bytes", e);
            } finally {
              // camera null should be checked, after releasing, this line will throw exception.
              current.addCallbackBuffer(bytes);
            }
          }
        }
      });
      camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(previewWidth, previewHeight)]);
      handler.handleCameraChanged(previewSize, new Size(width, height));
      camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
      Log.i(TAG, "## surface destroyed");
      closeCamera();
    }
  };

  private int getCameraId() {
    android.hardware.Camera.CameraInfo ci = new android.hardware.Camera.CameraInfo();
    for (int i = 0; i < android.hardware.Camera.getNumberOfCameras(); i++) {
      android.hardware.Camera.getCameraInfo(i, ci);
      if (ci.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) return i;
    }
    return -1;
  }

  private Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
    final int minSize = Math.max(Math.min(width, height), 320);
    final Size desiredSize = new Size(width, height);

    // Collect the supported resolutions that are at least as big as the preview Surface
    boolean exactSizeFound = false;
    final List<Size> bigEnough = new ArrayList<Size>();
    final List<Size> tooSmall = new ArrayList<Size>();
    for (final Size option : choices) {
      if (option.equals(desiredSize)) {
        // Set the size but don't return yet so that remaining sizes will still be logged.
        exactSizeFound = true;
      }

      if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
        bigEnough.add(option);
      } else {
        tooSmall.add(option);
      }
    }

    if (exactSizeFound) {
      return desiredSize;
    }

    // Pick the smallest of those, assuming we found any
    if (bigEnough.size() > 0) {
      final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
      Log.i(TAG, String.format("## choosen preview size:%s", chosenSize));
      return chosenSize;
    } else {
      Log.e(TAG, "## Couldn't find any suitable preview size");
      return choices[0];
    }
  }

  private class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(final Size lhs, final Size rhs) {
      return Long.signum(
              (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
  }
}
