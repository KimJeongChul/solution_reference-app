package org.tensorflow.lite.examples.detection;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;

import org.tensorflow.lite.examples.detection.customview.AutoFitTextureView;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.enums.FRAME_CONTROL;
import org.tensorflow.lite.examples.detection.enums.MODE;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.util.Map;

public class ViewManager {
    private static String TAG = ViewManager.class.toString();
    private CameraActivity view;
    private CameraViewHandler handler;

    private ImageButton actionButton;
    private ImageButton settingButton;
    private ImageButton modeChangeButton;
    private ImageView modeImage;
    private TextView modeMecText;
    private TextView modeLocalText;
    private TextView modelNameText;
    private TextView networkTimeText;
    private TextView inferenceTimeText;
    private LinearLayout timeLayout;
    private LinearLayout currentModelNameLayout;
    private FrameLayout mainLayout;
    private OverlayView drawView;
    private SurfaceView cameraView;
    private DrawHelper drawHelper;

    /* lottie */
    private LottieAnimationView circleLottie;
    private LottieAnimationView effectLottie;
    private LottieAnimationView transportLottie;

    /* animation */
    private Animation animSmaller;
    private Animation animSmallerReverse;
    private Animation animSmallerAndReverse;
    private Animation animBigger;
    private Animation animBiggerReverse;
    private Animation animRotateClockwise10;
    private Animation animRotateClockwise20;
    private Animation animRotateClockwise30;
    private Animation animRotateClockwise60;
    private Animation animRotateClockwise180;
    private Animation animChangeImageWithScale;
    private AnimationSet effectOn;
    private AnimationSet effectOff;

    /* color */
    private int SKOrange;
    private int LightGray;
    private MODE currentMode;

    private Object syncToken = new Object();
    private int previewWidth;
    private int previewHeight;
    private Map<DRAW_TYPE, Object> drawable;

    public ViewManager(CameraActivity view, CameraViewHandler handler) {
        this.view = view;
        this.handler = handler;
        drawHelper = new DrawHelper();
        initView();
        initHandlers();
    }

    public interface CameraViewHandler {
        public void modeChangeClicked();

        public void settingClicked();

        public void actionButtonClicked();
    }

    public SurfaceView getCameraView() {
        return this.cameraView;
    }

    public void setModelName(String name) {
        modelNameText.setText(name);
    }

    public void setMode(MODE mode) {
        if (mode != currentMode) {
            if (mode == MODE.LOCAL) {
                animateColor(modeMecText, "textColor", SKOrange, LightGray);
                animateColor(modeLocalText, "textColor", LightGray, SKOrange);
            } else {
                animateColor(modeMecText, "textColor", LightGray, SKOrange);
                animateColor(modeLocalText, "textColor", SKOrange, LightGray);
            }
            currentMode = mode;
            modeChangeButton.startAnimation(animRotateClockwise180);
            modeImage.startAnimation(animChangeImageWithScale);
        }
    }

    private void scheduleDrawing() {
        this.drawView.postInvalidate();
    }

    public void draw(InferInfo postprocessed) {
        draw(postprocessed.getPreviewSize().getWidth(), postprocessed.getPreviewSize().getHeight(),
                postprocessed.getDrawable());
        // this method should be called in thread
        synchronized (syncToken) {
            try {
                // should wait draw thread before finish drawing to cavas
                syncToken.wait();
            } catch (InterruptedException e) {
                Log.i(TAG, "## draw thread is killed");
            }
        }
    }

    public void drawWithoutWait(InferInfo postprocessed) {
        draw(postprocessed.getPreviewSize().getWidth(), postprocessed.getPreviewSize().getHeight(),
                postprocessed.getDrawable());
    }

    public void setBackground(Bitmap background) {
        drawHelper.setBackground(background);
        scheduleDrawing();
    }

    private void draw(int previewWidth, int previewHeight, Map<DRAW_TYPE, Object> drawable) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        this.drawable = drawable;
        scheduleDrawing();
    }

    public void clean() {
        // clear background
        setNetworkTime("-", "-");
        drawHelper.setBackground(null);
        this.drawable = null;
        scheduleDrawing();
    }

    public void displayTime(boolean enable) {
        if (enable) {
            timeLayout.setVisibility(LinearLayout.VISIBLE);
        } else {
            timeLayout.setVisibility(LinearLayout.GONE);
        }
    }

    public void displayMoelName(boolean enable) {
        if (enable) {
            currentModelNameLayout.setVisibility(LinearLayout.VISIBLE);
        } else {
            currentModelNameLayout.setVisibility(LinearLayout.GONE);
        }
    }

    public void changeActionButton(FRAME_CONTROL control, boolean detecting) {
        if (control == FRAME_CONTROL.PREVIEW) {
            actionButton.setImageDrawable(this.view.getDrawable(R.drawable.ic_shot_stream));
            if (detecting) {
                actionButton.startAnimation(animSmaller);
                effectLottie.startAnimation(effectOn);
                circleLottie.startAnimation(animRotateClockwise60);
            } else {
                actionButton.startAnimation(animSmallerReverse);
                effectLottie.startAnimation(effectOff);
                circleLottie.startAnimation(animRotateClockwise20);
            }
        } else if (control == FRAME_CONTROL.SINGLE) {
            actionButton.setImageDrawable(this.view.getDrawable(R.drawable.ic_shot_single));
            actionButton.startAnimation(animSmallerAndReverse);
        } else if (control == FRAME_CONTROL.PHOTOALBUM) {
            actionButton.setImageDrawable(this.view.getDrawable(R.drawable.ic_shot_single));
            actionButton.startAnimation(animSmallerAndReverse);
        }
    }

    public void setNetworkTime(String inference, String network) {
        this.view.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                networkTimeText.setText(network);
                inferenceTimeText.setText(inference);
            }
        });
    }

    private void initView() {
        actionButton = (ImageButton) find(R.id.actionBtn);
        settingButton = (ImageButton) find(R.id.settingBtn);
        modeChangeButton = (ImageButton) find(R.id.modeChangeBtn);
        modeImage = (ImageView) find(R.id.modeImage);
        modeMecText = (TextView) find(R.id.modeMecText);
        modeLocalText = (TextView) find(R.id.modeLocalText);
        modelNameText = (TextView) find(R.id.currentModelNameText);
        networkTimeText = (TextView) find(R.id.networkTimeText);
        inferenceTimeText = (TextView) find(R.id.inferenceTimeText);
        circleLottie = (LottieAnimationView) find(R.id.lottie_circle);
        effectLottie = (LottieAnimationView) find(R.id.lottie_effect);
        transportLottie = (LottieAnimationView) find(R.id.lottie_trasport);
        timeLayout = (LinearLayout) find(R.id.timeLayout);
        currentModelNameLayout = (LinearLayout) find(R.id.currentModelNameLayout);
        mainLayout = (FrameLayout) find(R.id.container);
        cameraView = (SurfaceView) find(R.id.texture);
        drawView = (OverlayView) find(R.id.tracking_overlay);
        drawView.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                // this will be called after calling scheduleDrawing()
                synchronized (syncToken) {

                    drawHelper.draw(canvas, previewWidth, previewHeight, drawable);
                    // drawing thread should be notified after finishing drawing to canvas
                    syncToken.notify();
                }
            }
        });

        modeImage.setImageDrawable(this.view.getDrawable(R.drawable.ic_mec));
        SKOrange = ContextCompat.getColor(this.view, R.color.SKOrange);
        LightGray = ContextCompat.getColor(this.view, R.color.LightGray);
        modeLocalText.setTextColor(LightGray);
        modeMecText.setTextColor(SKOrange);

        /* animation */
        animSmaller = AnimationUtils.loadAnimation(this.view, R.anim.smaller);
        animSmaller.setFillAfter(true);
        animSmallerReverse = AnimationUtils.loadAnimation(this.view, R.anim.smaller_reverse);
        animSmallerReverse.setFillAfter(true);
        animSmallerAndReverse = AnimationUtils.loadAnimation(this.view, R.anim.smaller_and_reverse);
        animBigger = AnimationUtils.loadAnimation(this.view, R.anim.bigger);
        animBigger.setFillAfter(true);
        animBiggerReverse = AnimationUtils.loadAnimation(this.view, R.anim.bigger_reverse);
        animBiggerReverse.setFillAfter(true);
        animRotateClockwise10 = AnimationUtils.loadAnimation(this.view, R.anim.rotate_clockwise_10);
        animRotateClockwise20 = AnimationUtils.loadAnimation(this.view, R.anim.rotate_clockwise_20);
        animRotateClockwise30 = AnimationUtils.loadAnimation(this.view, R.anim.rotate_clockwise_30);
        animRotateClockwise60 = AnimationUtils.loadAnimation(this.view, R.anim.rotate_clockwise_60);
        animRotateClockwise180 = AnimationUtils.loadAnimation(this.view, R.anim.rotate_clockwise_180);
        effectOn = new AnimationSet(false);
        effectOn.addAnimation(animBigger);
        effectOn.addAnimation(animRotateClockwise30);
        effectOff = new AnimationSet(false);
        effectOff.addAnimation(animBiggerReverse);
        effectOff.addAnimation(animRotateClockwise10);
        effectLottie.startAnimation(animRotateClockwise10);
        circleLottie.startAnimation(animRotateClockwise20);
        transportLottie.pauseAnimation();
    }

    public void showTransportIcon(boolean enable) {
        if (enable) {
            transportLottie.setVisibility(View.VISIBLE);
        } else {
            transportLottie.setVisibility(View.GONE);
        }
    }

    public void startTrasportAnimation() {
        transportLottie.playAnimation();
    }

    public void stopTrasportAnimation() {
        transportLottie.pauseAnimation();
        transportLottie.setProgress(0);
    }

    private <T extends View> T find(int id) {
        return this.view.findViewById(id);
    }

    private void initHandlers() {
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    handler.actionButtonClicked();
                } catch (Exception e) {
                    Log.e(TAG, "## fail to handle action button", e);
                }
            }
        });
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    handler.settingClicked();
                } catch (Exception e) {
                    Log.e(TAG, "## fail to handler setting clicked", e);
                }
            }
        });
        modeChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    handler.modeChangeClicked();
                } catch (Exception e) {
                    Log.e(TAG, "## fail to handler model change clicked", e);
                }
            }
        });
        animChangeImageWithScale = AnimationUtils.loadAnimation(this.view, R.anim.smaller_100_to_0);
        animChangeImageWithScale.setAnimationListener(new Animation.AnimationListener() {
            Animation animAfter = AnimationUtils.loadAnimation(view, R.anim.bigger_0_to_100);

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                changeModeImage();
                modeImage.startAnimation(animAfter);
            }
        });
    }

    private void changeModeImage() {
        Log.i("mode", "chagen image");
        if (currentMode == MODE.LOCAL) {
            modeImage.setImageDrawable(this.view.getDrawable(R.drawable.ic_local));
            Log.i("mode", "chagen image: local");
        }
        if (currentMode == MODE.MEC) {
            modeImage.setImageDrawable(this.view.getDrawable(R.drawable.ic_mec));
            Log.i("mode", "chagen image: mec");
        }
    }

    private void animateColor(TextView view, String propertyName, int startColor, int endColor) {
        PropertyValuesHolder textColorX = PropertyValuesHolder.ofInt(
                "textColor",
                startColor,
                endColor
        );
        textColorX.setEvaluator(new ArgbEvaluator());
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(view, textColorX);
        objectAnimator.setDuration(400);
        objectAnimator.start();
    }
}
