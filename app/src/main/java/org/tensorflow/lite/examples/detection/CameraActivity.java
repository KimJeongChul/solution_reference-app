package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.examples.detection.enums.DRAW_TYPE;
import org.tensorflow.lite.examples.detection.enums.FRAME_CONTROL;
import org.tensorflow.lite.examples.detection.enums.MODE;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Size;
import org.tensorflow.lite.examples.detection.inference.InferInfo;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.settings.DataBase;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;
import org.tensorflow.lite.examples.detection.settings.ModelSetting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class CameraActivity extends AppCompatActivity {

    private static String TAG = CameraActivity.class.toString();
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static int RESULT_LOAD_IMAGE = 1;

    private Size previewSize;

    private InferenceFlowManager inferenceManager;
    private CameraManager cameraManager;
    private ViewManager viewManager;
    private DataBase dataBase;
    private MODE mode;
    private MODEL_NAME modelName;
    private ModelSetting setting;
    private ModelInfo model;
    private InferenceData lastData;
    private boolean running;
    private boolean capturing;
    private Bitmap pickedPhoto;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        Log.i(TAG, "## on create camera activity");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        dataBase = new DataBase(this);
        running = false;
        mode = MODE.MEC;
    }

    private void initialize() {
        modelName = dataBase.loadCurrentModelName();
        setting = dataBase.loadModelSetting(modelName);
        model = ModelInfo.getModelInfo(setting, mode);


        if (viewManager == null) {
            viewManager = new ViewManager(this, viewHandler);
        }
        viewManager.setModelName(modelName.toString());

        if (cameraManager == null) {
            cameraManager = new CameraManager(viewManager.getCameraView(), handler);
        }

        if (inferenceManager == null) {
            inferenceManager = new InferenceFlowManager(this, inferenceHandler);
        }
        viewManager.changeActionButton(model.getFrameControl(), running);
        viewManager.setMode(mode);
        inferenceManager.start(setting, mode);
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
        Log.i(TAG, "## on start camera activity");
        ModelInfo.initialize();
        if (hasPermission()) {
            initialize();
        } else {
            requestPermission();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    private void disposeView() {
        running = false;
        capturing = false;
        pickedPhoto = null;
        viewManager.changeActionButton(model.getFrameControl(), running);
        viewManager.clean();
        viewManager.stopTrasportAnimation();
        inferenceManager.stop();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private InferenceFlowManager.InferenceFlowHandler inferenceHandler = new InferenceFlowManager.InferenceFlowHandler() {
        @Override
        public void handleInfeInfo(InferInfo info) {
            // if preview and rnning is false dont draw
            Log.i(TAG, String.format("## draw running:%s, capturing:%s, model:%s, device type:%s, " +
                            "mode:%s, frame control:%s, infer info:%s, info drawable:%s", running, capturing,
                    modelName, model.getDeviceType(),
                    mode, setting.getFrameControl(), info, info.getDrawable()));
            if (running) {
                viewManager.setNetworkTime(String.valueOf(info.getInferenceTime()),
                        String.valueOf(info.getNetworkTime()));
                viewManager.draw(info);
            } else if ((capturing && setting.getFrameControl() == FRAME_CONTROL.SINGLE)
                    || (capturing && setting.getFrameControl() == FRAME_CONTROL.PHOTOALBUM)) {
                viewManager.setNetworkTime(String.valueOf(info.getInferenceTime()),
                        String.valueOf(info.getNetworkTime()));

                Log.i(TAG, "## draw without wait");
                viewManager.drawWithoutWait(info);
                stopTrasportAnimation();
                if (modelName == MODEL_NAME.SUPERNOVA) {
                    // if super nova save photo to album
                    Map<DRAW_TYPE, Object> drawable = info.getDrawable();
                    if (info.getDrawable() != null && info.getDrawable().containsKey(DRAW_TYPE.IMAGE)) {
                        Bitmap image = (Bitmap) drawable.get(DRAW_TYPE.IMAGE);
                        saveImage(image);
                        toast("Picture Saved");
                    }
                }
            }
        }
    };

    private ViewManager.CameraViewHandler viewHandler = new ViewManager.CameraViewHandler() {
        @Override
        public void modeChangeClicked() {
            if (ModelInfo.supportLocal(modelName)) {
                viewManager.clean();
                if (mode == MODE.MEC) {
                    mode = MODE.LOCAL;
                    viewManager.showTransportIcon(false);
                } else {
                    mode = MODE.MEC;
                    viewManager.showTransportIcon(true);
                }
                model = ModelInfo.getModelInfo(setting, mode);
                inferenceManager.setMode(mode);
                viewManager.setMode(mode);
            }
        }

        @Override
        public void settingClicked() {
            Intent intent = new Intent(CameraActivity.this, SettingActivity.class);
            startActivity(intent);
        }

        @Override
        public void actionButtonClicked() {
            viewManager.clean();
            Log.i(TAG, String.format("## handler action button frame:%s, running:%s, captureing:%s",
                    setting.getFrameControl(), running, capturing));
            if (setting.getFrameControl() == FRAME_CONTROL.PREVIEW) {
                running = !running;
                if (!running) {
                    viewManager.clean();
                    stopTrasportAnimation();
                } else {
                    startTrasportAnimation();
                }
                Log.i(TAG, String.format("## preview mode running changed:%s", running));
            } else if (setting.getFrameControl() == FRAME_CONTROL.SINGLE) {
                // don't use running to prevent to feed camera view
                // and when only capturing is true, draw will work to prevent previous running camera
                // view drawing
                if (capturing) {
                    viewManager.clean();
                    stopTrasportAnimation();
                    cameraManager.playCamera();
                } else {
                    cameraManager.stopCamera();
                    // feed last data
                    startTrasportAnimation();
                    inferenceManager.feed(lastData);
                }
                capturing = !capturing;
            } else if (setting.getFrameControl() == FRAME_CONTROL.PHOTOALBUM) {
                // this is to prevent to draw last running result
                capturing = true;
                cameraManager.stopCamera();
                loadAlbum();
                // transport animation will be started in camera changed callback
            }
            viewManager.changeActionButton(setting.getFrameControl(), running);
        }
    };

    private CameraManager.CameraHandler handler = new CameraManager.CameraHandler() {
        @Override
        public void handleBytes(byte[] bytes) {
            lastData =
                    InferenceData.builder().data(bytes).previewSize(previewSize).orientated(90).model(model).build();
            if (previewSize != null && running) {
                inferenceManager.feed(lastData);
            }
        }

        @Override
        public void handleCameraChanged(Size size, Size canvasSize) {
            Log.i(TAG, String.format("## handle camera size:%s", size));
            previewSize = size;
            Log.i(TAG, "## view resumed");
            if (pickedPhoto != null) {
                // draw album photo
                Log.i(TAG, "## picked photo is not null, so draw it");

                // picked photo should be resized before starting inference
                // the input image size should be same as canvas view
                capturing = true;
                org.opencv.core.Size dst = new org.opencv.core.Size(canvasSize.getWidth(),
                        canvasSize.getHeight());
                Bitmap resized = ImageUtils.resizeKeepAspectRatio(pickedPhoto, dst);

                // original picked photo should be set for background, it will be ratio resized by
                // drawing helper
                // the preview size of image should be same as canvas view
                viewManager.setBackground(pickedPhoto);
                InferenceData data;
                if (modelName == MODEL_NAME.SUPERNOVA) {
                    // super nova need original image
                    // because it's result is just double sized image
                    // so double sized image will be resized to canvas
                    // the ratio with background is same as double size image drawn canvas
                    data = InferenceData.builder()
                            .model(model)
                            .image(pickedPhoto)
                            .orientated(0)
                            .previewSize(new Size(pickedPhoto.getWidth(), pickedPhoto.getHeight())).build();
                } else {
                    data = InferenceData.builder()
                            .model(model)
                            .image(resized)
                            .orientated(0)
                            .previewSize(canvasSize).build();
                }
                startTrasportAnimation();
                inferenceManager.feed(data);
                pickedPhoto = null;
            }
        }

        @Override
        public void handleCameraDisposed() {
            Log.i(TAG, String.format("## handle camera disposed"));
            disposeView();
        }
    };

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                initialize();
            } else {
                requestPermission();
            }
        }
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        if (resultCode == RESULT_OK && reqCode == RESULT_LOAD_IMAGE) {
            try {
                Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                pickedPhoto = getRotatedBitmap(selectedImage, imageUri);
                Log.i(TAG, String.format("## picked photo width:%s, height:%s", pickedPhoto.getWidth(), pickedPhoto.getHeight()));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "## fail to load selected photo", e);
            }
        } else {
            Log.w(TAG, "## you haven't picked image");
        }
    }

    private void loadAlbum() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMAGE);
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        CameraActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap, Uri uri) {
        try (InputStream inputStream = this.getContentResolver().openInputStream(uri)) {
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);

                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);

                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    return bitmap;
            }
        } catch (IOException e) {
            Log.e(TAG, "fail to rotate bitmap", e);
        }
        return null;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public void toast(String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = getLayoutInflater();
                View toastDesign = inflater.inflate(R.layout.custom_toast, (ViewGroup)findViewById(R.id.custom_toast_root));
                TextView text = toastDesign.findViewById(R.id.custom_toast_message);
                text.setText(content);
                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(toastDesign);
                toast.show();
            }
        });
    }

    public void startTrasportAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewManager.startTrasportAnimation();
            }
        });
    }

    public void stopTrasportAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewManager.stopTrasportAnimation();
            }
        });
    }

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = getCurrentTime() + ".png";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            MediaScannerConnection.scanFile(this, new String[] {file.getPath()}, new String[] {
                    "image/png"}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date today = Calendar.getInstance().getTime();
        return dateFormat.format(today);
    }
}
