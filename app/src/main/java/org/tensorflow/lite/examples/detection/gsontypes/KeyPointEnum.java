package org.tensorflow.lite.examples.detection.gsontypes;

import com.google.gson.annotations.SerializedName;

public class KeyPointEnum {
    @SerializedName("nose")
    public KeyPointPair nose;
    @SerializedName("leftEye")
    public KeyPointPair leftEye;
    @SerializedName("rightEye")
    public KeyPointPair rightEye;
    @SerializedName("leftEar")
    public KeyPointPair leftEar;
    @SerializedName("rightEar")
    public KeyPointPair rightEar;
    @SerializedName("leftShoulder")
    public KeyPointPair leftShoulder;
    @SerializedName("rightShoulder")
    public KeyPointPair rightShoulder;
    @SerializedName("leftElbow")
    public KeyPointPair leftElbow;
    @SerializedName("rightElbow")
    public KeyPointPair rightElbow;
    @SerializedName("leftWrist")
    public KeyPointPair leftWrist;
    @SerializedName("rightWrist")
    public KeyPointPair rightWrist;
    @SerializedName("leftHip")
    public KeyPointPair leftHip;
    @SerializedName("rightHip")
    public KeyPointPair rightHip;
    @SerializedName("leftKnee")
    public KeyPointPair leftKnee;
    @SerializedName("rightKnee")
    public KeyPointPair rightKnee;
    @SerializedName("leftAnkle")
    public KeyPointPair leftAnkle;
    @SerializedName("rightAnkle")
    public KeyPointPair rightAnkle;

    public KeyPointPair getNose() {
        return nose;
    }

    public KeyPointPair getLeftEye() {
        return leftEye;
    }

    public KeyPointPair getRightEye() {
        return rightEye;
    }

    public KeyPointPair getLeftEar() {
        return leftEar;
    }

    public KeyPointPair getRightEar() {
        return rightEar;
    }

    public KeyPointPair getLeftShoulder() {
        return leftShoulder;
    }

    public KeyPointPair getRightShoulder() {
        return rightShoulder;
    }

    public KeyPointPair getLeftElbow() {
        return leftElbow;
    }

    public KeyPointPair getRightElbow() {
        return rightElbow;
    }

    public KeyPointPair getLeftWrist() {
        return leftWrist;
    }

    public KeyPointPair getRightWrist() {
        return rightWrist;
    }

    public KeyPointPair getLeftHip() {
        return leftHip;
    }

    public KeyPointPair getRightHip() {
        return rightHip;
    }

    public KeyPointPair getLeftKnee() {
        return leftKnee;
    }

    public KeyPointPair getRightKnee() {
        return rightKnee;
    }

    public KeyPointPair getLeftAnkle() {
        return leftAnkle;
    }

    public KeyPointPair getRightAnkle() {
        return rightAnkle;
    }

}
