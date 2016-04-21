package edu.tufts.cs.thejigisup;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TheJigIsUp::Main";

    static {
        // Thanks a Million to http://stackoverflow.com/a/27421494
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "Static OpenCV Initialization Failed");
        } else {
            Log.i(TAG, "Static OpenCV Initialization Success");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button newPuzzleButton = (Button)findViewById(R.id.newPuzzleButton);
        Button existingPuzzleButton = (Button)findViewById(R.id.existingPuzzleButton);

        newPuzzleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getBaseContext(), PhotoActivity.class);
                intent.putExtra("activityMode", "PUZZLE_BOX");
                startActivity(intent);

            }
        });

        existingPuzzleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getBaseContext(), BoxGalleryActivity.class);
                startActivity(intent);

            }
        });




    }

}
