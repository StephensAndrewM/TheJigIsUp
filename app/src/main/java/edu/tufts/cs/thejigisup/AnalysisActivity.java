package edu.tufts.cs.thejigisup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static org.opencv.features2d.Features2d.drawMatches;

public class AnalysisActivity extends AppCompatActivity {

    private static final String TAG = "Jig::AnalysisActivity";

    private final int CIRCLE_RADIUS = 30;
    private final Scalar CIRCLE_COLOR = new Scalar(0,255,0,0.5);

    String boxImageFile;
    String pieceImageFile;
    Mat originalBoxMatBgr;

    ArrayList<Pair<Double, Point>> resultantMatches = null;
    int resultantMatchIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // Load the Piece Images from File
        Bundle extras = getIntent().getExtras();
        boxImageFile = extras.getString("boxImage");
        pieceImageFile = extras.getString("pieceImage");

        // Display Images Temporarily (With No Result)
        Mat boxMatBgr = Imgcodecs.imread(boxImageFile, Imgcodecs.IMREAD_COLOR);
        originalBoxMatBgr = boxMatBgr;
        Mat boxMatRgb = new Mat();
        Imgproc.cvtColor(boxMatBgr, boxMatRgb, Imgproc.COLOR_BGR2RGB);
        ImageView boxImageView = (ImageView) findViewById(R.id.display_box);
        displayMatImage(boxImageView, boxMatRgb);

        Mat pieceMatBgr = Imgcodecs.imread(pieceImageFile, Imgcodecs.IMREAD_COLOR);
        Mat pieceMatRgb = new Mat();
        Imgproc.cvtColor(pieceMatBgr, pieceMatRgb, Imgproc.COLOR_BGR2RGB);
        ImageView pieceImageView = (ImageView) findViewById(R.id.display_piece);
        displayMatImage(pieceImageView, pieceMatRgb);

        Log.d(TAG, "Starting Asynchronous Thread");
        new AsyncPuzzleActivity(this).execute(boxImageFile, pieceImageFile);

    }

    private void displayMatImage(ImageView view, Mat rgb) {

        Bitmap displayImage;

        try{
            Log.i(TAG, "Displaying Box Bitmap");
            displayImage = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgb, displayImage);
            view.setImageBitmap(displayImage);
        }
        catch (CvException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    private void displayBoxResult(Mat boxMatBgr, Point bestPosition) {

        // Draw Indicator of Position
        Mat boxMatRgb = new Mat();
        Imgproc.cvtColor(boxMatBgr, boxMatRgb, Imgproc.COLOR_BGR2RGB);
        Imgproc.circle(boxMatRgb, bestPosition, CIRCLE_RADIUS, CIRCLE_COLOR, -1);

        ImageView displayImageView = (ImageView) findViewById(R.id.display_box);
        displayMatImage(displayImageView, boxMatRgb);

    }

    public void tryAgainClick(View v) {

        Button tryAgain = (Button) findViewById(R.id.try_again_button);

        resultantMatchIndex++;

        if (resultantMatches != null && resultantMatchIndex < resultantMatches.size()) {

            displayBoxResult(originalBoxMatBgr, resultantMatches.get(resultantMatchIndex).second);

            if (resultantMatchIndex >= resultantMatches.size()) {
                Log.d(TAG, "Hiding Try Again Button (A) at ResultantMatchIndex "+resultantMatchIndex);
                tryAgain.setVisibility(View.INVISIBLE);
            }

        } else {
            Log.d(TAG, "Hiding Try Again Button (B) at ResultantMatchIndex "+resultantMatchIndex);
            tryAgain.setVisibility(View.INVISIBLE);
        }

    }

    public void matchAnotherClick(View v) {

        // Go back to camera view with same box image
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        startActivity(intent);

    }

    // We don't want to go back to previous activity. Force not to.
    @Override
    public void onBackPressed() {}


    private class AsyncPuzzleActivity extends AsyncTask<String, Void, ArrayList<Pair<Double, Point>>> {

        private static final String TAG = "Jig::AsyncPuzzleAct";

        private final int MAX_DIVISIONS = 4;
        private final double SCORE_THRESHOLD = 5;

        FeatureDetector detector;
        DescriptorExtractor extractor;
        DescriptorMatcher matcher;

        // Save Variables Across Points in Thread
        Mat boxMatBgr;
        ProgressDialog progress;

        public AsyncPuzzleActivity(Activity parentActivity) {
            progress = new ProgressDialog(parentActivity);
        }

        @Override
        protected void onPreExecute() {

            Log.d(TAG, "Preparing Background Processing");

            progress.setTitle("Processing...");
            progress.setMessage("Please wait while we analyze your puzzle.");
            progress.setCancelable(false);
            progress.setCanceledOnTouchOutside(false);
            progress.show();

        }

        @Override
        protected ArrayList<Pair<Double, Point>> doInBackground(String... params) {

            Log.d(TAG, "Starting Background Processing");

            String boxImageFile = params[0];
            String pieceImageFile = params[1];

            Log.i(TAG, "Loading Box Image: " + boxImageFile);
            boxMatBgr = Imgcodecs.imread(boxImageFile, Imgcodecs.IMREAD_COLOR);
            Mat boxMatBW = new Mat();
            Imgproc.cvtColor(boxMatBgr, boxMatBW, Imgproc.COLOR_BGR2GRAY);

            Log.i(TAG, "Loading Piece Image: " + pieceImageFile);
            Mat pieceMat = Imgcodecs.imread(pieceImageFile, Imgcodecs.IMREAD_GRAYSCALE);

            // Get the Target Piece Position
            ArrayList<Pair<Double, Point>> bestPositions = processImages(boxMatBW, pieceMat);

            return bestPositions;

        }

        @Override
        protected void onPostExecute(ArrayList<Pair<Double, Point>> bestPositions) {

            Log.d(TAG, "Finished Image Processing");

            if (bestPositions != null) {
                displayBoxResult(boxMatBgr, bestPositions.get(0).second);

                Toast toast = Toast.makeText(getApplicationContext(), "We found your piece! It's at the green dot on the box.", Toast.LENGTH_LONG);
                toast.show();

                // Only give the user the option to try again if there are multiple results
                if (bestPositions.size() > 1) {
                    Log.d(TAG, "Displaying Try Again Button");
                    Button tryAgain = (Button) findViewById(R.id.try_again_button);
                    tryAgain.setVisibility(View.VISIBLE);
                    resultantMatches = bestPositions;
                    resultantMatchIndex = 0;
                }

            } else {
                Log.d(TAG, "NO POSITIONS FOUND");

                Toast toast = Toast.makeText(getApplicationContext(), "Sorry! We couldn't find your piece. Try another one?", Toast.LENGTH_LONG);
                toast.show();

            }

            if (progress != null && progress.isShowing()) {
                progress.dismiss();
                progress = null;
            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {}

        ArrayList<Pair<Double, Point>> processImages(Mat box, Mat piece) {

            // Initialize OpenCV Items
            detector = FeatureDetector.create(FeatureDetector.ORB);
            extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            ArrayList<Pair<Double, Point>> scores = new ArrayList<>();

            for (int divisions = 1; divisions <= MAX_DIVISIONS; divisions++) {
                Log.i(TAG, "Attempting " + divisions + " divisions");

                // for number of quadrants
                for (int quad = 0; quad < divisions * divisions; quad++) {
                    Log.i(TAG, "Attempting quadrant " + quad);

                    // divide into quadrant
                    Mat currQuad = getQuadrant(box, quad, divisions);
                    // get score for quadrant
                    Pair<Double, Point> quadrantScore = findMatches(currQuad, piece, divisions, quad);

                    // If we found any matching points, record that (with translated coordinates)
                    if (quadrantScore.first > 0) {
                        Pair<Double, Point> savedScore = Pair.create(quadrantScore.first,
                                translatePosition(quadrantScore.second, divisions, quad, box));
                        scores.add(savedScore);
                    }

                }

                if (scores.size() > 0) {

                    // Custom comparator, compares only the score values of the points
                    Collections.sort(scores, new Comparator<Pair<Double, Point>>() {
                        @Override
                        public int compare(Pair<Double, Point> lhs, Pair<Double, Point> rhs) {
                            return rhs.first.compareTo(lhs.first);
                        }
                    });

                    // If the best score has been found, we don't need to loop again
                    if (scores.get(0).first >= SCORE_THRESHOLD) {
                        Log.i(TAG, "Score matches best score, exiting");
                        break;
                    }

                }

            }

            // If we didn't find anything, return a null value
            if (scores.size() == 0) {
                return null;
            }

            double bestScore = scores.get(0).first;
            Point bestPosition = scores.get(0).second;
            Log.i(TAG, "Final Score: " + bestScore);
            Log.i(TAG, "Final Position: " + bestPosition.x + ", " + bestPosition.y);

            ArrayList<Pair<Double, Point>> returnedScores = new ArrayList<>();

            // If the best score is good, we return only the scores above the threshold
            if (bestScore >= SCORE_THRESHOLD) {
                for (int i = 0; i < scores.size(); i++) {
                    if (scores.get(i).first >= SCORE_THRESHOLD) {
                        returnedScores.add(scores.get(i));
                    }
                }

            // If the best score is meh, we return other points that have that same score
            } else {
                for (int i = 0; i < scores.size(); i++) {
                    if (scores.get(i).first >= bestScore) {
                        returnedScores.add(scores.get(i));
                    }
                }
            }

            Log.d(TAG, scores.toString());
            Log.d(TAG, returnedScores.toString());

            return returnedScores;

        }

        Point translatePosition(Point relativePosition, int divisions, int quad, Mat box) {
            int row = quad / divisions;
            int col = quad % divisions;

            int qWidth = box.cols() / divisions;
            int qHeight = box.rows() / divisions;

            return new Point(col * qWidth + relativePosition.x, row * qHeight + relativePosition.y);
        }

        Mat getQuadrant(Mat box, int quadrantId, int divisions) {

            int row = quadrantId / divisions;
            int col = quadrantId % divisions;

            int qWidth = box.cols() / divisions;
            int qHeight = box.rows() / divisions;

            Log.i(TAG, "Returning Quadrant " + quadrantId + " of size " + qWidth + "x" + qHeight + " at position (" + row + "," + col + ")");

            Mat quadrant = new Mat(box, new Rect(col*qWidth, row*qHeight, qWidth, qHeight));
            return quadrant;

        }

        Pair<Double, Point> findMatches(Mat boxRegion, Mat piece, int divisions, int quadrantId) {

            Pair<Mat, MatOfKeyPoint> boxFeaturePointData = getFeaturePoints(boxRegion);
            Mat boxDescriptors = boxFeaturePointData.first;
            MatOfKeyPoint boxKeypoints = boxFeaturePointData.second;

            Pair<Mat, MatOfKeyPoint> pieceFeaturePointData = getFeaturePoints(piece);
            Mat pieceDescriptors = pieceFeaturePointData.first;
            MatOfKeyPoint pieceKeypoints = pieceFeaturePointData.second;

            MatOfDMatch matches = new MatOfDMatch();
            matcher.match(boxDescriptors, pieceDescriptors, matches);

            // Check Each Match, Make Sure It's Within Reasonable Hamming Distance
            double max_dist = 0;
            double min_dist = 100;
            List<DMatch> matchesList = matches.toList();
            Log.i(TAG, "Found " + matchesList.size() + " Matches with " + pieceDescriptors.rows() + " Piece Descriptors");
            for (int i = 0; i < matchesList.size(); i++) {
                Double distance = (double) matchesList.get(i).distance;
                if (distance < min_dist) min_dist = distance;
                if (distance > max_dist) max_dist = distance;
            }
            Log.i(TAG, "Min Hamming Distance: " + min_dist);
            Log.i(TAG, "Max Hamming Distance: " + max_dist);
            Log.i(TAG, "Hamming Cutoff: " + Math.min(3*min_dist, 25));
            Log.i(TAG, "Divisions: " + divisions + " Quadrant: " + quadrantId);

            Vector<DMatch> listOfGoodMatches = new Vector<>();
            for (int i = 0; i < matchesList.size(); i++) {
                if (matchesList.get(i).distance < Math.min(3 * min_dist, 25)) {
                    listOfGoodMatches.add(matchesList.get(i));
                }
            }
            Log.i(TAG, "Found " + listOfGoodMatches.size() + " Good Matches");
            MatOfDMatch goodMatches = new MatOfDMatch();
            goodMatches.fromList(listOfGoodMatches);

            // Debug: Output the Matches
            Mat matchesMat = new Mat();
            drawMatches(boxRegion, boxKeypoints, piece, pieceKeypoints, goodMatches, matchesMat);
            saveImage(matchesMat, divisions, quadrantId);


            // Calculate the Center of Matches, if a Match is Found
            int numMatches = listOfGoodMatches.size();
            if (numMatches == 0) {
                return Pair.create(0.0, new Point(-1, -1));
            }
            KeyPoint keyPoints[] = boxKeypoints.toArray();
            Point temp;
            int totalx = 0;
            int totaly = 0;
            for(int i = 0; i < numMatches; i++) {
                temp = keyPoints[listOfGoodMatches.get(i).queryIdx].pt;
                totalx += temp.x;
                totaly += temp.y;
            }
            int avgx = totalx / numMatches;
            int avgy = totaly / numMatches;

            return Pair.create((double)numMatches, new Point(avgx, avgy));

        }

        private void saveImage(Mat mBgr, int divisions, int quadrantId) {

            long saveTime = System.currentTimeMillis();

            File sd = Environment.getExternalStorageDirectory();

            File debugDirectory = new File(sd, "jig/debug/");
            debugDirectory.mkdirs();
            String filename = saveTime + "-" + divisions + "-" + quadrantId + ".png";
            File file = new File(debugDirectory, filename);

            String filePath = file.getAbsolutePath();
            Boolean imageSaved = Imgcodecs.imwrite(filePath, mBgr);

            if (imageSaved) {
                Log.i(TAG, "SUCCESS writing debug image: " + filePath);
            } else {
                Log.i(TAG, "FAILED writing debug image: " + filePath);
            }

        }

        Pair<Mat, MatOfKeyPoint> getFeaturePoints(Mat image) {

            MatOfKeyPoint keypoints = new MatOfKeyPoint();
            detector.detect(image, keypoints);
            Mat descriptors = new Mat();
            extractor.compute(image, keypoints, descriptors);

            return Pair.create(descriptors, keypoints);

        }

    }

}
