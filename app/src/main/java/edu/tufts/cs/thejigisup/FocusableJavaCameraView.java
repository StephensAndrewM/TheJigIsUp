package edu.tufts.cs.thejigisup;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

public class FocusableJavaCameraView extends JavaCameraView {

    private static final String TAG = "Jig::FocusableCamera";

    public FocusableJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean initializeCamera(int width, int height) {
        boolean result = super.initializeCamera(width, height);

        if (result) {
            // Continuous Picture is a Better Mode than Continuous Video (the Default)
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
        }

        return result;
    }

    public void autoFocus() {
        Log.d(TAG, "Focusing Camera");
        mCamera.autoFocus(null);
    }

}