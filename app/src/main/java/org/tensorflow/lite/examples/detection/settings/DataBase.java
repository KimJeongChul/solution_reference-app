package org.tensorflow.lite.examples.detection.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import org.tensorflow.lite.examples.detection.enums.COLOR_FORMAT;
import org.tensorflow.lite.examples.detection.enums.FRAME_CONTROL;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.enums.PROTOCOL;

import static android.content.Context.MODE_PRIVATE;

public class DataBase {
    Context context;
    private String TAG = "DataBaseController";

    public DataBase(Context context) {
        this.context = context;
    }

    public void saveModelSetting(MODEL_NAME name, ModelSetting setting) {
        SharedPreferences prefs = context.getSharedPreferences(name.toString(), MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("irt_display", setting.isIrtDisplay());
        editor.putBoolean("model_name_display", setting.isModelNameDisplay());
        editor.putString("frame_control", setting.getFrameControl().toString());
        editor.putString("address", setting.getAddress());
        editor.putString("token", setting.getToken());
        editor.putString("color_format", setting.getColorFormat().toString());
        editor.putString("protocol", setting.getProtocol().toString());
        editor.apply();
    }

    public ModelSetting loadModelSetting(MODEL_NAME name) {
        SharedPreferences prefs = context.getSharedPreferences(name.toString(), MODE_PRIVATE);
        ModelSetting setting = new ModelSetting();
        setting.setIrtDisplay(prefs.getBoolean("irt_display", true));
        setting.setModelNameDisplay(prefs.getBoolean("model_name_display", true));
        setting.setFrameControl(FRAME_CONTROL.valueOf(prefs.getString("frame_control",
                FRAME_CONTROL.PREVIEW.toString())));
        setting.setAddress(prefs.getString("address", ""));
        setting.setToken(prefs.getString("token", ""));
        setting.setColorFormat(COLOR_FORMAT.valueOf(prefs.getString("color_format",
                COLOR_FORMAT.RGB.toString())));
        setting.setProtocol(PROTOCOL.valueOf(prefs.getString("protocol",
                PROTOCOL.HTTP.toString())));
        setting.setModelName(name);
        return setting;
    }

    public void saveCurrentModelName(MODEL_NAME modelName) {
        SharedPreferences prefs = context.getSharedPreferences("current_model", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", modelName.toString());
        editor.apply();
    }

    public MODEL_NAME loadCurrentModelName() {
        SharedPreferences prefs = context.getSharedPreferences("current_model", MODE_PRIVATE);
        String currentModelName = prefs.getString("name", MODEL_NAME.POSENET.toString());
        return MODEL_NAME.valueOf(currentModelName.toUpperCase());
    }
}
