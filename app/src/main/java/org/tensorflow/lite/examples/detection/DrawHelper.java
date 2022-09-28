package org.tensorflow.lite.examples.detection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;


import org.opencv.core.Point;
import org.opencv.core.Size;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.drawtypes.BoxRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.CircleRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LabelRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.LineRecognition;
import org.tensorflow.lite.examples.detection.drawtypes.MaskRecognition;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.List;
import java.util.Map;

public class DrawHelper {

  private static String TAG = DrawHelper.class.toString();

  private Paint boxPaint = new Paint();
  private Paint circlePaint = new Paint();
  private Paint linePaint = new Paint();
  private Paint maskPaint = new Paint();
  private Paint labelPaint = new Paint();
  private Bitmap background;
  private static final int[] COLORS = {
          Color.BLUE,
          Color.RED,
          Color.GREEN,
          Color.YELLOW,
          Color.CYAN,
          Color.MAGENTA,
          Color.WHITE,
          Color.parseColor("#55FF55"),
          Color.parseColor("#FFA500"),
          Color.parseColor("#FF8888"),
          Color.parseColor("#AAAAFF"),
          Color.parseColor("#FFFFAA"),
          Color.parseColor("#55AAAA"),
          Color.parseColor("#AA33AA"),
          Color.parseColor("#0D0068")
  };

  private void initializeBoxPaint(Paint boxPaint) {
    boxPaint.setColor(Color.BLUE);
    boxPaint.setStyle(Paint.Style.STROKE);
    boxPaint.setStrokeWidth(6.0f);
    boxPaint.setStrokeCap(Paint.Cap.ROUND);
    boxPaint.setStrokeJoin(Paint.Join.ROUND);
    boxPaint.setStrokeMiter(100);
  }

  private void initializeLabelPaint(Paint labelPaint) {
    labelPaint.setColor(Color.BLUE);
    labelPaint.setStyle(Paint.Style.STROKE);
    labelPaint.setStrokeWidth(6.0f);
    labelPaint.setStrokeCap(Paint.Cap.ROUND);
    labelPaint.setStrokeJoin(Paint.Join.ROUND);
    labelPaint.setStrokeMiter(100);
  }

  private void initializeCirclePaint(Paint circlePaint) {
    circlePaint.setColor(Color.CYAN);
    circlePaint.setStrokeWidth(6.0f);
    circlePaint.setStrokeCap(Paint.Cap.ROUND);
    circlePaint.setStyle(Paint.Style.STROKE);
    circlePaint.setStrokeJoin(Paint.Join.ROUND);
  }

  private void initializeLinePaint(Paint linePaint) {
    linePaint.setColor(Color.CYAN);
    linePaint.setStrokeWidth(6.0f);
    linePaint.setStyle(Paint.Style.FILL);
  }

  public DrawHelper() {
    initializeBoxPaint(boxPaint);
    initializeCirclePaint(circlePaint);
    initializeLinePaint(linePaint);
    initializeLabelPaint(labelPaint);
  }

  public void setBackground(final Bitmap background) {
    this.background = background;
  }

  public void draw(final Canvas canvas, int previewWidth, int previewHeight, Map<DRAW_TYPE,
          Object> drawable) {
    Matrix mat =
            ImageUtils.getTransformationMatrix(
                    previewWidth,
                    previewHeight,
                    canvas.getWidth(),
                    canvas.getHeight(),
                    0,
                    false);

    if (background != null) {
      Bitmap image = ImageUtils.resizeKeepAspectRatio(background, new Size(canvas.getWidth(),
              canvas.getHeight()));
      Log.i(TAG, String.format("## resize image width:%s, height:%s", image.getWidth(),
              image.getHeight()));
      canvas.drawBitmap(image, 0, 0, null);
    }

    Log.i(TAG, String.format("## draw drawable:%s", drawable));
    if (drawable != null) {
      try {
        if (drawable.containsKey(DRAW_TYPE.IMAGE)) {
          Bitmap image =
                  (Bitmap) drawable.get(DRAW_TYPE.IMAGE);
          Log.i(TAG, String.format("## try to draw image width:%s, height:%s", image.getWidth(),
                  image.getHeight()));
          image = ImageUtils.resizeKeepAspectRatio(image, new Size(canvas.getWidth(),
                  canvas.getHeight()));
          Log.i(TAG, String.format("## resize image width:%s, height:%s", image.getWidth(),
                  image.getHeight()));
          canvas.drawBitmap(image, 0, 0, null);
        }

        if (drawable.containsKey(DRAW_TYPE.LINE)) {
          drawLine(canvas, mat, (List<LineRecognition>) drawable.get(DRAW_TYPE.LINE));
        }

        if (drawable.containsKey(DRAW_TYPE.CIRCLE)) {
          drawCircle(canvas, mat, (List<CircleRecognition>) drawable.get(DRAW_TYPE.CIRCLE));
        }


        if (drawable.containsKey(DRAW_TYPE.RECT)) {
          drawRect(canvas, mat, (List<BoxRecognition>) drawable.get(DRAW_TYPE.RECT));
        }

        if (drawable.containsKey(DRAW_TYPE.LABEL)) {
          drawLabel(canvas, mat, (List<LabelRecognition>) drawable.get(DRAW_TYPE.LABEL));
        }

        if (drawable.containsKey(DRAW_TYPE.MASK)) {
          drawMask(canvas, mat, (List<MaskRecognition>) drawable.get(DRAW_TYPE.MASK));
        }
      } catch (Exception e) {
        Log.e(TAG, "draw: exception: " + e);
      }
    }
  }

  public void draw(final Canvas canvas, InferInfo postprocessed) {
    Log.i(TAG, String.format("## draw canvas width:%s, height:%s, preview width:%s, height:%s",
            canvas.getWidth(), canvas.getHeight(), postprocessed.getPreviewSize().getWidth(),
            postprocessed.getPreviewSize().getHeight()));
    int previewWidth = postprocessed.getPreviewSize().getWidth();
    int previewHeight = postprocessed.getPreviewSize().getHeight();
    Map<DRAW_TYPE, Object> drawable = postprocessed.getDrawable();
    draw(canvas, previewWidth, previewHeight, drawable);
  }

  private synchronized void drawCircle(Canvas canvas,
                                       Matrix mat, List<CircleRecognition> circles) {
    if (circles.size() > 0) {
      for (CircleRecognition result : circles) {
        float[] point = {result.pointX, result.pointY};
        mat.mapPoints(point);
        canvas.drawCircle(point[0], point[1], 10, circlePaint);
      }
    }
  }

  private synchronized void drawMask(Canvas canvas,
                                     Matrix mat, List<MaskRecognition> maskRecognitions) {
    if (maskRecognitions.size() > 0) {

      for (MaskRecognition maskRecognition : maskRecognitions) {
        List<Point[]> contourPoints = maskRecognition.getContourLine();
        Path path = new Path();
        float[] startPoint = {(float) contourPoints.get(0)[0].x, (float) contourPoints.get(0)[0].y};
        mat.mapPoints(startPoint);
        path.moveTo(startPoint[0], startPoint[1]);
        for (Point[] pointsPerContour : contourPoints) {
          for (Point point : pointsPerContour) {
            float[] resizedPoint = {(float) point.x, (float) point.y};
            mat.mapPoints(resizedPoint);
            path.lineTo(resizedPoint[0], resizedPoint[1]);
          }
        }

        maskPaint.setStrokeJoin(Paint.Join.ROUND);
        maskPaint.setColor(COLORS[maskRecognition.getColor() % 15]);
        maskPaint.setAntiAlias(true);
        maskPaint.setStyle(Paint.Style.STROKE); // 선이 그려지도록
        maskPaint.setStrokeWidth(5f); // 선의 굵기 지정
        canvas.drawPath(path, maskPaint);

        maskPaint.setAlpha(90); //투명하게
        maskPaint.setStyle(Paint.Style.FILL_AND_STROKE); // 채우기 옵션
        canvas.drawPath(path, maskPaint);

      }
    }
  }

  private synchronized void drawRect(Canvas canvas,
                                     Matrix mat, List<BoxRecognition> boxRecognitions) {
    if (boxRecognitions.size() > 0) {

      for (final BoxRecognition result : boxRecognitions) {
        if (result.getLocation() == null) {
          continue;
        }
        final RectF srcRect = new RectF(result.getLocation());
        final RectF detectionScreenRect = new RectF();
        mat.mapRect(detectionScreenRect, srcRect);
        boxPaint.setColor(COLORS[result.getColor() % 15]);

        canvas.drawRect(detectionScreenRect, boxPaint);
      }

    }
  }

  private synchronized void drawLine(Canvas canvas,
                                     Matrix mat, List<LineRecognition> lineRecognitions) {
    if (lineRecognitions.size() > 0) {
      for (final LineRecognition result : lineRecognitions) {
        float[] pointSrc = {result.startX, result.startY};
        float[] pointDst = {result.stopX, result.stopY};
        mat.mapPoints(pointSrc);
        mat.mapPoints(pointDst);
        canvas.drawLine(pointSrc[0], pointSrc[1], pointDst[0], pointDst[1], linePaint);
      }
    }
  }

  private synchronized void drawLabel(Canvas canvas,
                                      Matrix mat, List<LabelRecognition> labelRecognitions) {
    if (labelRecognitions.size() > 0) {
      BorderedText borderedText = new BorderedText(50.0f);
      for (final LabelRecognition result : labelRecognitions) {
        float[] startPoint = {result.posX, result.posY};
        mat.mapPoints(startPoint);
        labelPaint.setColor(COLORS[result.getColorIndex() % 15]);
        borderedText.drawText(
                canvas, startPoint[0], startPoint[1] - 10.0f, result.toString(), labelPaint);
      }
    }
  }

}
