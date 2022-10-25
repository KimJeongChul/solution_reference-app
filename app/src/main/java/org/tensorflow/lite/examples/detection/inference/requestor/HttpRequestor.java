package org.tensorflow.lite.examples.detection.inference.requestor;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.enums.DEVICE_TYPE;
import org.tensorflow.lite.examples.detection.enums.MODEL_NAME;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.settings.RequestAddress;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRequestor implements Requestor {

    private static String TAG = "RemoteInferencer";

    private RequestAddress address;
    private URL httpURL;

    public HttpRequestor(RequestAddress address) {
        this.address = address;
    }

    private Map<String, String> getHeaders(InferenceData data) {
        Map<String, String> headers = new HashMap<>();
        if (this.address.getHttpToken() != null && this.address.getHttpToken() != "") {
            headers.put("Authorization", "Bearer " + this.address.getHttpToken());
        }
        if (data.getDeviceType() == DEVICE_TYPE.AIX &&
                (data.getModelName() == MODEL_NAME.YOLOV3 ||
                        data.getModelName() == MODEL_NAME.YOLOV4 ||
                        data.getModelName() == MODEL_NAME.POSENET)) {
            headers.put("Content-Type", "application/octet-stream");
            headers.put("Accept", "application/octet-stream");
            headers.put("InferRequest", "{'batch_size': 1, 'input': {'name': 'INPUT0'}, 'output':" +
                    " [{'name': 'OUTPUT0'}, {'name': 'OUTPUT1'}, {'name': 'OUTPUT2'}, {'name': 'OUTPUT3'}]}");

        }
        return headers;
    }

    @Override
    public InferInfo inference(InferenceData data) {
        if (data.getDeviceType().equals(DEVICE_TYPE.GPU)) {
            return requestGpuServer(data);
        } else {
            data.addHeaders(getHeaders(data));
            return requestAixServer(data);
        }
    }

    private InferInfo requestAixServer(InferenceData data) {
        byte[] bytes = data.getData();
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/octet-stream"),
                bytes);

        OkHttpClient client = new OkHttpClient();
        String address = this.address.getHttpAddress();
        Request.Builder builder =
                new Request.Builder().url(address).post(body);
        for (String key : data.getHeaders().keySet()) {
            builder.addHeader(key, data.getHeaders().get(key));
        }
        Request request = builder.build();
        try {
            Log.i(TAG, "## test");
            Log.i(TAG, "## request headers: " + request.headers());
            Response response = client.newCall(request).execute();
            Log.i(TAG, "## response headers: " + response.headers());
            InferInfo info = new InferInfo();
            Map<String, String> headers = new HashMap<>();
            for (String key : response.headers().names()) {
                headers.put(key, response.header(key));
            }
            info.setResponseHeaders(headers);
            info.setInferenceResult(response.body().bytes());
            return info;
        } catch (Exception e) {
            Log.e(TAG, "Response err " + e);
            return null;
        }
    }

    private InferInfo requestGpuServer(InferenceData data) {
        byte[] bytes = data.getData();
        String encoded = Base64.getEncoder().encodeToString(bytes);
        Map<String, Object> jsonParams = new HashMap<>();
        jsonParams.put("data", encoded);

        OkHttpClient client = new OkHttpClient();
        String address = this.address.getHttpAddress();
        Request request =
                new Request.Builder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .url(address)
                        .post(RequestBody.create(MediaType.parse("application/json"),
                                (new JSONObject(jsonParams)).toString()))
                        .build();

        try {
            Response response = client.newCall(request).execute();

            JsonParser parser = new JsonParser();
            String bodyString = response.body().string();
            Log.i(TAG, "## body:" + bodyString);
            JsonObject root = (JsonObject) parser.parse(bodyString);
            long serverTime = (long) root.get("inference_time").getAsInt();
            String res = root.get("result").getAsString();
            InferInfo info = new InferInfo();
            info.setInferenceTime(serverTime);
            info.setInferenceResult(res);
            return info;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void openChannel() {
    }

    @Override
    public void closeChannel() {
    }
}
