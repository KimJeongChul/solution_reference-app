package org.tensorflow.lite.examples.detection;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.tensorflow.lite.examples.detection.enums.COLOR_FORMAT;
import org.tensorflow.lite.examples.detection.enums.FRAME_CONTROL;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.enums.PROTOCOL;
import org.tensorflow.lite.examples.detection.settings.DataBase;
import org.tensorflow.lite.examples.detection.settings.ModelSetting;

public class SettingActivity extends Activity {
    private DataBase dataBase;
    private ModelSetting posenetSetting;
    private ModelSetting yolov3Setting;
    private ModelSetting segmentSetting;
    private ModelSetting supernovaSetting;
    private MODEL_NAME currentModelName;
    private ModelSetting mSetting;
    private ImageButton goBackButton;
    private EditText addressEdit;
    private EditText tokenEdit;
    private Switch irtDisplay;
    private Switch modelNameDisplay;
    private RadioButton posenetRadio;
    private RadioButton yolov3Radio;
    private RadioButton segmentRadio;
    private RadioButton supernovaRadio;
    private RadioButton streamRadio;
    private RadioButton singleRadio;
    private RadioButton photoAlbumRadio;
    private RadioButton rgbRadio;
    private RadioButton bgrRadio;
    private RadioButton httpRadio;
    private RadioButton grpcRadio;
    private Button saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        goBackButton = findViewById(R.id.btn_go_back);
        posenetRadio = findViewById(R.id.radio_posenet);
        yolov3Radio = findViewById(R.id.radio_yolov3);
        segmentRadio = findViewById(R.id.radio_segment);
        supernovaRadio = findViewById(R.id.radio_supernova);
        irtDisplay = findViewById(R.id.swt_display_irt);
        modelNameDisplay = findViewById(R.id.swt_display_model_name);
        addressEdit = findViewById(R.id.edit_address);
        tokenEdit = findViewById(R.id.edit_token);
        streamRadio = findViewById(R.id.radio_stream);
        singleRadio = findViewById(R.id.radio_single);
        photoAlbumRadio = findViewById(R.id.radio_photo);
        rgbRadio = findViewById(R.id.radio_rgb);
        bgrRadio = findViewById(R.id.radio_bgr);
        httpRadio = findViewById(R.id.radio_http);
        grpcRadio = findViewById(R.id.radio_grpc);
        saveButton = findViewById(R.id.settingSaveBtn);

        irtDisplay.setOnCheckedChangeListener(handleChangeSwitch);
        modelNameDisplay.setOnCheckedChangeListener(handleChangeSwitch);
        goBackButton.setOnClickListener(handleOnClick);
        saveButton.setOnClickListener(handleOnClick);

        addressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSetting.setAddress(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        tokenEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                mSetting.setToken(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        dataBase = new DataBase(SettingActivity.this);
        posenetSetting = dataBase.loadModelSetting(MODEL_NAME.POSENET);
        yolov3Setting = dataBase.loadModelSetting(MODEL_NAME.YOLOV3);
        segmentSetting = dataBase.loadModelSetting(MODEL_NAME.SEGMENTATION);
        supernovaSetting = dataBase.loadModelSetting(MODEL_NAME.SUPERNOVA);
        currentModelName = dataBase.loadCurrentModelName();
        loadSetting(currentModelName);
    }

    private void saveSetting() {
        dataBase.saveCurrentModelName(currentModelName);
        dataBase.saveModelSetting(MODEL_NAME.POSENET, posenetSetting);
        dataBase.saveModelSetting(MODEL_NAME.YOLOV3, yolov3Setting);
        dataBase.saveModelSetting(MODEL_NAME.SEGMENTATION, segmentSetting);
        dataBase.saveModelSetting(MODEL_NAME.SUPERNOVA, supernovaSetting);
    }

    private void loadSetting(MODEL_NAME name) {
        if (name == MODEL_NAME.POSENET) {
            mSetting = posenetSetting;
            posenetRadio.setChecked(true);
        }
        if (name == MODEL_NAME.YOLOV3) {
            mSetting = yolov3Setting;
            yolov3Radio.setChecked(true);
        }
        if (name == MODEL_NAME.SEGMENTATION) {
            mSetting = segmentSetting;
            segmentRadio.setChecked(true);
        }
        if (name == MODEL_NAME.SUPERNOVA) {
            mSetting = supernovaSetting;
            supernovaRadio.setChecked(true);
        }
        currentModelName = name;
        addressEdit.setText(mSetting.getAddress());
        tokenEdit.setText(mSetting.getToken());
        if (mSetting.getFrameControl() == FRAME_CONTROL.PREVIEW) {
            streamRadio.setChecked(true);
        } else if (mSetting.getFrameControl() == FRAME_CONTROL.SINGLE) {
            singleRadio.setChecked(true);
        } else {
            photoAlbumRadio.setChecked(true);
        }
        if (mSetting.getColorFormat() == COLOR_FORMAT.RGB) {
            rgbRadio.setChecked(true);
        } else {
            bgrRadio.setChecked(true);
        }
        if (mSetting.getProtocol() == PROTOCOL.HTTP) {
            httpRadio.setChecked(true);
        } else {
            grpcRadio.setChecked(true);
        }
        irtDisplay.setChecked(mSetting.isIrtDisplay());
        modelNameDisplay.setChecked(mSetting.isModelNameDisplay());
    }

    CompoundButton.OnCheckedChangeListener handleChangeSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int id = buttonView.getId();
            if (id == R.id.swt_display_irt) {
                mSetting.setIrtDisplay(isChecked);
            } else if (id == R.id.swt_display_model_name) {
                mSetting.setModelNameDisplay(isChecked);
            }
        }
    };

    View.OnClickListener handleOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_go_back) {
                finish();
            } else if (id == R.id.settingSaveBtn) {
                saveSetting();
                toast("Setting Saved");
                finish();
            }
        }
    };

    public void toast(String content) {
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

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        int id = view.getId();

        if (!checked) {
            return;
        }

        if (id == R.id.radio_http) {
            mSetting.setProtocol(PROTOCOL.HTTP);
        } else if (id == R.id.radio_grpc) {
            mSetting.setProtocol(PROTOCOL.GRPC);
        } else if (id == R.id.radio_rgb) {
            mSetting.setColorFormat(COLOR_FORMAT.RGB);
        } else if (id == R.id.radio_bgr) {
            mSetting.setColorFormat(COLOR_FORMAT.BGR);
        } else if (id == R.id.radio_stream) {
            mSetting.setFrameControl(FRAME_CONTROL.PREVIEW);
        } else if (id == R.id.radio_single) {
            mSetting.setFrameControl(FRAME_CONTROL.SINGLE);
        } else if (id == R.id.radio_photo) {
            mSetting.setFrameControl(FRAME_CONTROL.PHOTOALBUM);
        } else if (id == R.id.radio_posenet) {
            loadSetting(MODEL_NAME.POSENET);
        } else if (id == R.id.radio_yolov3) {
            loadSetting(MODEL_NAME.YOLOV3);
        } else if (id == R.id.radio_segment) {
            loadSetting(MODEL_NAME.SEGMENTATION);
        } else if (id == R.id.radio_supernova) {
            loadSetting(MODEL_NAME.SUPERNOVA);
        }
    }
}
