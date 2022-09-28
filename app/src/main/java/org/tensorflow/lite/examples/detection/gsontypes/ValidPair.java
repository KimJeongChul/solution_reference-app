package org.tensorflow.lite.examples.detection.gsontypes;

import androidx.annotation.NonNull;

public class ValidPair {
    public ValidPair(int aId, int bId, float score){
        this.aId = aId;
        this.bId = bId;
        this.score = score;
    }
    int aId;
    int bId;
    float score;

    @NonNull
    @Override
    public String toString() {
        return "("+aId+", "+bId+")";
    }

    public int getaId() {
        return aId;
    }

    public int getbId() {
        return bId;
    }
}
