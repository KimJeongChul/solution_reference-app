package org.tensorflow.lite.examples.detection.drawtypes;

import android.graphics.RectF;

public class BoxRecognition
{
    private RectF location;
    private int colorIndex;

    public BoxRecognition(final RectF location, int colorIndex) {
        this.location = location;
        this.colorIndex = colorIndex;
    }

    public RectF getLocation() {
        return new RectF(location);
    }

    public int getColor(){
        return colorIndex;
    }

    public void setLocation(RectF location) {
        this.location = location;
    }

}
