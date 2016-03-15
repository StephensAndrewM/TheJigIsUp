package edu.tufts.cs.thejigisup;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TheJigIsUp::Main";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV Initialization Failed");
        } else {
            Log.i(TAG, "OpenCV Initialization Success");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

}
