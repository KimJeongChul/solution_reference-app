
package org.tensorflow.lite.examples.detection.env;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ImageUtils {
  private static String TAG = ImageUtils.class.toString();
  // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
  // are normalized to eight bits.
  static final int kMaxChannelValue = 262143;

  public static int getYUVByteSize(final int width, final int height) {
    // The luminance plane requires 1 byte per pixel.
    final int ySize = width * height;

    // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
    // Each 2x2 block takes 2 bytes to encode, one each for U and V.
    final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

    return ySize + uvSize;
  }

  public static void saveBitmap(final Bitmap bitmap) {
    saveBitmap(bitmap, "preview.png");
  }

  public static void saveBitmap(final Bitmap bitmap, final String filename) {
    final String root = Environment.getExternalStorageDirectory().toString();

    final String fname = filename;
    final File file = new File(root, fname);
    if (file.exists()) {
      file.delete();
    }
    try {
        Log.i("saveBitmap", "saveBitmap: success!!");
      final FileOutputStream out = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
      out.flush();
      out.close();

    } catch (final Exception e) {
      Log.e("saveBitmap", "Exception!");
    }
  }

  public static void convertYUV420SPToARGB8888(byte[] input, int width, int height, int[] output) {
    final int frameSize = width * height;
    for (int j = 0, yp = 0; j < height; j++) {
      int uvp = frameSize + (j >> 1) * width;
      int u = 0;
      int v = 0;

      for (int i = 0; i < width; i++, yp++) {
        int y = 0xff & input[yp];
        //get y value, stored in the first block of data
        //0xff is needed to deal with the signed issue

        if ((i & 1) == 0) {
          //get u and v vlaues stored after y value one per 2*2 block of pixels, interleaved.
          //prepare them a
          v = 0xff & input[uvp++];
          u = 0xff & input[uvp++];
        }

        output[yp] = YUV2RGB(y, u, v);
      }
    }
  }

  public static void convertYUV420SPToBGR(byte[] input, int width, int height, int[] output) {
    final int frameSize = width * height;
    for (int j = 0, yp = 0; j < height; j++) {
      int uvp = frameSize + (j >> 1) * width;
      int u = 0;
      int v = 0;

      for (int i = 0; i < width; i++, yp++) {
        int y = 0xff & input[yp];
        //get y value, stored in the first block of data
        //0xff is needed to deal with the signed issue

        if ((i & 1) == 0) {
          //get u and v vlaues stored after y value one per 2*2 block of pixels, interleaved.
          //prepare them a
          v = 0xff & input[uvp++];
          u = 0xff & input[uvp++];
        }

        output[yp] = YUV2BGR(y, u, v);
      }
    }
  }

  private static int YUV2RGB(int y, int u, int v) {
    // Adjust and check YUV values
    y = (y - 16) < 0 ? 0 : (y - 16);
    u -= 128;
    v -= 128;

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 2.018 * nU);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 1.596 * nV);
    int y1192 = 1192 * y;
    int r = (y1192 + 1634 * v);
    int g = (y1192 - 833 * v - 400 * u);
    int b = (y1192 + 2066 * u);

    // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
    r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
    g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
    b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

    return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
  }

  private static int YUV2BGR(int y, int u, int v){
    // Adjust and check YUV values
    y = (y - 16) < 0 ? 0 : (y - 16);
    u -= 128;
    v -= 128;

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 2.018 * nU);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 1.596 * nV);
    int y1192 = 1192 * y;
    int r = (y1192 + 1634 * v);
    int g = (y1192 - 833 * v - 400 * u);
    int b = (y1192 + 2066 * u);

    // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
    r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
    g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
    b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

    return 0xff000000 | ((b << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((r >> 10) & 0xff);

  }

  public static void convertYUV420ToARGB8888(
      byte[] yData,
      byte[] uData,
      byte[] vData,
      int width,
      int height,
      int yRowStride,
      int uvRowStride,
      int uvPixelStride,
      int[] out) {
    int yp = 0;
    for (int j = 0; j < height; j++) {
      int pY = yRowStride * j;
      int pUV = uvRowStride * (j >> 1);

      for (int i = 0; i < width; i++) {
        int uv_offset = pUV + (i >> 1) * uvPixelStride;

        out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
      }
    }
  }

  public static Matrix getTransformationMatrix(
      final int srcWidth,
      final int srcHeight,
      final int dstWidth,
      final int dstHeight,
      final int applyRotation,
      final boolean maintainAspectRatio) {
    final Matrix matrix = new Matrix();

    if (applyRotation != 0) {
      if (applyRotation % 90 != 0) {
        Log.i(TAG, String.format("Rotation of %d % 90 != 0", applyRotation));
      }

      // Translate so center of image is at origin.
      matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

      // Rotate around origin.
      matrix.postRotate(applyRotation);
    }

    // Account for the already applied rotation, if any, and then determine how
    // much scaling is needed for each axis.
    final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

    final int inWidth = transpose ? srcHeight : srcWidth;
    final int inHeight = transpose ? srcWidth : srcHeight;

    // Apply scaling if necessary.
    if (inWidth != dstWidth || inHeight != dstHeight) {
      final float scaleFactorX = dstWidth / (float) inWidth;
      final float scaleFactorY = dstHeight / (float) inHeight;

      if (maintainAspectRatio) {
        // Scale by minimum factor so that dst is filled completely while
        // maintaining the aspect ratio. Some image may fall off the edge.
        final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
        matrix.postScale(scaleFactor, scaleFactor);
      } else {
        // Scale exactly to fill dst from src.
        matrix.postScale(scaleFactorX, scaleFactorY);
      }
    }

    if (applyRotation != 0) {
      // Translate back from origin centered reference to destination frame.
      matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
    }

    return matrix;
  }

  public static byte[] bitmapToByteArray( Bitmap $bitmap ) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
    $bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
    byte[] byteArray = stream.toByteArray() ;
    return byteArray ;
  }

  public static Bitmap byteArrayToBitmap(byte[] $byteArray ) {
    Bitmap bitmap = BitmapFactory.decodeByteArray( $byteArray, 0, $byteArray.length ) ;
    return bitmap ;
  }

  public static Bitmap resizeKeepAspectRatio(Bitmap image, Size dstSize) {
    Mat argb = new Mat();
    org.opencv.android.Utils.bitmapToMat(image, argb);

    Mat input = new Mat(); // input is rgb
    Imgproc.cvtColor(argb, input, Imgproc.COLOR_RGBA2RGB);
    Mat output = new Mat();
    double h1 = dstSize.width * (input.rows() / (double) input.cols());
    double w2 = dstSize.height * (input.cols() / (double) input.rows());
    if (h1 <= dstSize.height) {
      Imgproc.resize(input, output, new org.opencv.core.Size(dstSize.width, h1));
    } else {
      Imgproc.resize(input, output, new Size(w2, dstSize.height));
    }
    int top = (int) (dstSize.height - output.rows()) / 2;
    int down = (int) (dstSize.height - output.rows() + 1) / 2;
    int left = (int) (dstSize.width - output.cols()) / 2;
    int right = (int) (dstSize.width - output.cols() + 1) / 2;
    Core.copyMakeBorder(output, output, top, down, left, right, Core.BORDER_CONSTANT,
            new Scalar(0,0,0));
    Bitmap result = Bitmap.createBitmap((int) dstSize.width, (int) dstSize.height,
            Bitmap.Config.ARGB_8888);
    org.opencv.android.Utils.matToBitmap(output, result);
    return result;
  }
}
