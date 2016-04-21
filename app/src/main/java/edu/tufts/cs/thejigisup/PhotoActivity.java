package edu.tufts.cs.thejigisup;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class PhotoActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "Jig::PhotoActivity";

//    private CameraBridgeViewBase mOpenCvCameraView;
    private FocusableJavaCameraView mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(PhotoActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public PhotoActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    // Hold mode, remember what we're taking a picture of
    private enum ActivityMode {
        PUZZLE_PIECES, PUZZLE_BOX
    }
    private ActivityMode mode;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_photo);

        // Hide status bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Initialize camera view
        mOpenCvCameraView  = (FocusableJavaCameraView) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // Get data given from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getString("activityMode") != null
                && extras.getString("activityMode").equals("PUZZLE_BOX")) {
            mode = ActivityMode.PUZZLE_BOX;
        } else {
            mode = ActivityMode.PUZZLE_PIECES;
        }

        // Display the right label in the top left
        ImageView cameraLabel;
        if (mode == ActivityMode.PUZZLE_BOX) {
            cameraLabel = (ImageView) findViewById(R.id.boxLabel);
        } else {
            cameraLabel = (ImageView) findViewById(R.id.pieceLabel);
        }
        cameraLabel.setVisibility(View.VISIBLE);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    boolean touched = false;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mOpenCvCameraView.autoFocus();
        return true;
    }

    // Called by Capture Button (see View)
    public void onCapturePress(View v) {
        touched = true;
        Toast toast = Toast.makeText(getApplicationContext(), "Picture Captured!", Toast.LENGTH_SHORT);
        toast.show();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();

        if (touched) {
            Log.i(TAG, "Screen Touched");
            touched = false;

            String imageFilename = saveImage(rgba);

            Intent intent;
            if (mode == ActivityMode.PUZZLE_BOX) {
                intent = new Intent(getBaseContext(), PhotoActivity.class);
                intent.putExtra("activityMode", "PUZZLE_PIECES");
                intent.putExtra("boxImage", imageFilename);
            } else {
                intent = new Intent(getBaseContext(), AnalysisActivity.class);
                intent.putExtra("pieceImage", imageFilename);

                Bundle extras = getIntent().getExtras();
                String boxImageFilename = extras.getString("boxImage");
                intent.putExtra("boxImage", boxImageFilename);
            }

            startActivity(intent);

        }

        return rgba;
    }

    private String saveImage(Mat mRgba) {

        Mat mBgr = new Mat();
        Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);

        Mat mBgrMini = new Mat();
        Size sz = new Size(200,200);
        Imgproc.resize( mBgr, mBgrMini, sz );

        long saveTime = System.currentTimeMillis();

        // Get Path Directories, Creating Folders if Necessary
        File sd = Environment.getExternalStorageDirectory();
        File pieceDirectory = new File(sd, "jig/pieces/");
        File boxDirectory = new File(sd, "jig/box/");
        File smallBoxDirectory = new File(sd, "jig/box/small/");
        pieceDirectory.mkdirs();
        boxDirectory.mkdirs();
        smallBoxDirectory.mkdirs();

        // Save in Separate Folder Based on Mode
        String imageFilename = saveTime + ".png";
        File imageFile;
        File smallImageFile = null;
        if (mode == ActivityMode.PUZZLE_PIECES) {
            imageFile = new File(pieceDirectory, imageFilename);
        } else {
            imageFile = new File(boxDirectory, imageFilename);
            smallImageFile = new File(smallBoxDirectory, imageFilename);
        }

        imageFilename = imageFile.getAbsolutePath();
        Log.i(TAG, "Saving Image: " + imageFilename);
        Boolean imageSaved = Imgcodecs.imwrite(imageFilename, mBgr);
        if (imageSaved) {
            Log.i(TAG, "SUCCESS writing image to external storage");
        } else {
            Log.i(TAG, "Fail writing image to external storage");
        }
        if(mode == ActivityMode.PUZZLE_BOX) {
            String smallFilePath = smallImageFile.getAbsolutePath();
            Boolean smallImageSaved = Imgcodecs.imwrite(smallFilePath, mBgrMini);

            if (smallImageSaved) {
                Log.i(TAG, "SUCCESS writing small image to external storage");
            } else {
                Log.i(TAG, "Fail writing smallimage to external storage");
            }
        }
        return imageFilename;

    }

}