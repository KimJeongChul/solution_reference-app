package org.tensorflow.lite.examples.detection.settings;

import org.tensorflow.lite.examples.detection.enums.DEVICE_TYPE;
import org.tensorflow.lite.examples.detection.enums.FRAME_CONTROL;
import org.tensorflow.lite.examples.detection.enums.MODE;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.enums.PROTOCOL;
import org.tensorflow.lite.examples.detection.enums.VERSION;
import org.tensorflow.lite.examples.detection.env.Size;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ModelInfo {

    private static int LOCAL_CROP_SIZE = 608;
    private static int POSENET_AIX_CROP_SIZE = 513;
    private static int POSENET_GPU_CROP_SIZE = 513;
    private static int YOLOV3_AIX_CROP_SIZE = 608;
    private static int YOLOV3_GPU_CROP_SIZE = 416;
    private static int YOLOV2_AIX_CROP_SIZE = 608;
    private static int YOLOV2_GPU_CROP_SIZE = 608;
    private static int SUPERNOVA_AIX_CROP_WIDTH = 720;
    private static int SUPERNOVA_AIX_CROP_HEIGHT = 540;
    private static int SUPERNOVA_AIX_CROP_SIZE = -1;
    private static int SUPERNOVA_GPU_CROP_SIZE = -1;
    private static int SEGMENTATION_AIX_CROP_SIZE = 608;
    private static int SEGMENTATION_GPU_CROP_SIZE = 608;
    private static Map<MODEL_NAME, Map<DEVICE_TYPE, Integer>> cropMap;
    private static Map<MODEL_NAME, DEVICE_TYPE> deviceTypeMap;
    private static Map<MODEL_NAME, VERSION> versionMap;

    private MODEL_NAME modelName;
    private RequestAddress address;
    private PROTOCOL protocol;
    private DEVICE_TYPE deviceType;
    private MODE mode;
    private FRAME_CONTROL frameControl;
    private VERSION version;
    private Size cropSize;

    public static void initialize() {
        versionMap = new HashMap<>();
        cropMap = new HashMap<>();
        cropMap.put(MODEL_NAME.POSENET, new HashMap<>());
        cropMap.put(MODEL_NAME.YOLOV2, new HashMap<>());
        cropMap.put(MODEL_NAME.YOLOV3, new HashMap<>());
        cropMap.put(MODEL_NAME.SEGMENTATION, new HashMap<>());
        cropMap.put(MODEL_NAME.SUPERNOVA, new HashMap<>());

        cropMap.get(MODEL_NAME.POSENET).put(DEVICE_TYPE.AIX, POSENET_AIX_CROP_SIZE);
        cropMap.get(MODEL_NAME.POSENET).put(DEVICE_TYPE.GPU, POSENET_GPU_CROP_SIZE);
        cropMap.get(MODEL_NAME.YOLOV2).put(DEVICE_TYPE.AIX, YOLOV2_AIX_CROP_SIZE);
        cropMap.get(MODEL_NAME.YOLOV2).put(DEVICE_TYPE.GPU, YOLOV2_GPU_CROP_SIZE);
        cropMap.get(MODEL_NAME.YOLOV3).put(DEVICE_TYPE.AIX, YOLOV3_AIX_CROP_SIZE);
        cropMap.get(MODEL_NAME.YOLOV3).put(DEVICE_TYPE.GPU, YOLOV3_GPU_CROP_SIZE);
        cropMap.get(MODEL_NAME.SUPERNOVA).put(DEVICE_TYPE.AIX, SUPERNOVA_AIX_CROP_SIZE);
        cropMap.get(MODEL_NAME.SUPERNOVA).put(DEVICE_TYPE.GPU, SUPERNOVA_GPU_CROP_SIZE);
        cropMap.get(MODEL_NAME.SEGMENTATION).put(DEVICE_TYPE.AIX, SEGMENTATION_AIX_CROP_SIZE);
        cropMap.get(MODEL_NAME.SEGMENTATION).put(DEVICE_TYPE.GPU, SEGMENTATION_GPU_CROP_SIZE);
        // device type map, this is configuration to select inference server
        deviceTypeMap = new HashMap<>();
        deviceTypeMap.put(MODEL_NAME.POSENET, DEVICE_TYPE.AIX);
        deviceTypeMap.put(MODEL_NAME.YOLOV3, DEVICE_TYPE.AIX);
        deviceTypeMap.put(MODEL_NAME.YOLOV2, DEVICE_TYPE.AIX);
        deviceTypeMap.put(MODEL_NAME.SEGMENTATION, DEVICE_TYPE.AIX);
        deviceTypeMap.put(MODEL_NAME.SUPERNOVA, DEVICE_TYPE.AIX);
        versionMap.put(MODEL_NAME.POSENET, VERSION.V2003);
        versionMap.put(MODEL_NAME.YOLOV2, VERSION.V2003);
        versionMap.put(MODEL_NAME.YOLOV3, VERSION.V2003);
        versionMap.put(MODEL_NAME.SUPERNOVA, VERSION.V2106);
    }

    private static Size getCropSize(MODEL_NAME modelName, MODE mode, DEVICE_TYPE deviceType) {
        if (mode.equals(MODE.LOCAL)) {
            return new Size(LOCAL_CROP_SIZE, LOCAL_CROP_SIZE);
        } else {
            if (modelName.equals(MODEL_NAME.YOLOV3)) {
                // because gpu version is only yolov2
                int size = cropMap.get(MODEL_NAME.YOLOV2).get(deviceType);
                return new Size(size, size);
            } else if (modelName.equals(MODEL_NAME.SUPERNOVA)) {
                return new Size(SUPERNOVA_AIX_CROP_WIDTH, SUPERNOVA_AIX_CROP_HEIGHT);
            } else {
                int size = cropMap.get(modelName).get(deviceType);
                return new Size(size, size);
            }
        }
    }

    private static DEVICE_TYPE getDeviceType(MODEL_NAME modelName) {
        return deviceTypeMap.get(modelName);
    }

    public static boolean supportLocal(MODEL_NAME modelName) {
        if (modelName.equals(MODEL_NAME.SUPERNOVA)) {
            return false;
        } else {
            return true;
        }
    }

    public static ModelInfo getModelInfo(ModelSetting setting, MODE mode) {
        DEVICE_TYPE deviceType = getDeviceType(setting.getModelName());
        Size cropSize = getCropSize(setting.getModelName(), mode, deviceType);
        RequestAddress address = new RequestAddress();
        address.setHttpAddress(setting.getAddress());
        address.setHttpToken(setting.getToken());
        PROTOCOL protocol = setting.getProtocol();
        ModelInfo modelInfo = ModelInfo.builder()
                .deviceType(deviceType)
                .cropSize(cropSize)
                .version(versionMap.get(setting.getModelName()))
                .mode(mode)
                .modelName(setting.getModelName())
                .address(address)
                .protocol(protocol)
                .frameControl(setting.getFrameControl())
                .build();
        return modelInfo;
    }
}
