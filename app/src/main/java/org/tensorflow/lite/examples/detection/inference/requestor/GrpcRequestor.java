package org.tensorflow.lite.examples.detection.inference.requestor;

import android.util.Log;

import com.google.protobuf.ByteString;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.inference.InferenceData;
import org.tensorflow.lite.examples.detection.settings.ModelInfo;
import org.tensorflow.lite.examples.detection.settings.RequestAddress;
import org.tensorflow.lite.examples.detection.inference.InferInfo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.inference.InferenceRequest;
import io.grpc.examples.inference.InferenceResponse;
import io.grpc.examples.inference.InferencerGrpc;

public class GrpcRequestor implements Requestor {

    private static String TAG = GrpcRequestor.class.toString();

    private RequestAddress address;
    private ManagedChannel channel;

    public GrpcRequestor(RequestAddress address) {
        this.address = address;
    }

    @Override
    public InferInfo inference(InferenceData data) {
        ByteString byteString = ByteString.copyFrom(data.getData());
        if(channel != null){
            synchronized (channel){
                try{
                    InferencerGrpc.InferencerBlockingStub stub = InferencerGrpc.newBlockingStub(channel);
                    InferenceRequest request =
                            InferenceRequest.newBuilder().setModel(data.getModel().getModelName().name()).setData(byteString).build();
                    InferenceResponse response = stub.inference(request);
                    String result = response.getResult();
                    long serverTime  = response.getInferenceTime();
                    InferInfo info = new InferInfo();
                    info.setInferenceResult(result);
                    info.setInferenceTime(serverTime);
                    return info;
                }catch (StatusRuntimeException e){
                    Log.e(TAG, "getInferenceResultGrpc: "+e );
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void openChannel() {
        String[] hostAndPort = null;
        try {
            hostAndPort = parseGrpcAddress(address.getGrpcAddress());
            channel = ManagedChannelBuilder.forAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])).usePlaintext().build();
            Log.e(TAG, "openChannel: "+hostAndPort[0]+":"+hostAndPort[1]);
        }catch (IllegalArgumentException e) {
            Log.e(TAG, "openChannel: "+e );
            channel = null;
        }catch (ArrayIndexOutOfBoundsException e){
            Log.e(TAG, "openChannel: "+e );
            channel = null;
        }
    }

    @Override
    public void closeChannel() {
        if (channel != null) {
            channel.shutdown();
            channel = null;
        }
    }

    private String[] parseGrpcAddress(String grpcAddress){
        String[] hostAndPort = grpcAddress.split(":");
        return hostAndPort;
    }
}
