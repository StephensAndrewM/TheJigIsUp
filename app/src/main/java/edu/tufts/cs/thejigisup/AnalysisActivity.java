package edu.tufts.cs.thejigisup;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;

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
import java.util.List;
import java.util.Vector;

import static org.opencv.features2d.Features2d.drawMatches;

public class AnalysisActivity extends AppCompatActivity {

    private static final String TAG = "Jig::AnalysisActivity";

    private final int MAX_DIVISIONS = 4;
    private final double SCORE_THRESHOLD = 5;
    private final int CIRCLE_RADIUS = 20;
    private final Scalar CIRCLE_COLOR = new Scalar(0,255,0);

    FeatureDetector detector;
    DescriptorExtractor extractor;
    DescriptorMatcher matcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // Load the Piece Images from File
        Bundle extras = getIntent().getExtras();
        String boxImageFile = extras.getString("boxImage");
        String pieceImageFile = extras.getString("pieceImage");

        Log.i(TAG, "Loading Box Image: " + boxImageFile);
        Mat boxMatBgr = Imgcodecs.imread(boxImageFile, Imgcodecs.IMREAD_COLOR);
        Mat boxMatBW = new Mat();
        Imgproc.cvtColor(boxMatBgr, boxMatBW, Imgproc.COLOR_BGR2GRAY);

        Log.i(TAG, "Loading Piece Image: " + pieceImageFile);
        Mat pieceMat = Imgcodecs.imread(pieceImageFile, Imgcodecs.IMREAD_GRAYSCALE);

        // Get the Target Piece Position
        Point bestPosition = processImages(boxMatBW, pieceMat);

        // Draw Indicator of Position
        Mat boxMatRgb = new Mat();
        Imgproc.cvtColor(boxMatBgr, boxMatRgb, Imgproc.COLOR_BGR2RGB);
        Imgproc.circle(boxMatRgb, bestPosition, CIRCLE_RADIUS, CIRCLE_COLOR, -1);

        Bitmap displayImage;
        ImageView displayImageView;
        displayImageView = (ImageView) findViewById(R.id.display_image);

        try{
            Log.i(TAG, "Displaying Box Bitmap");
            displayImage = Bitmap.createBitmap(boxMatRgb.cols(), boxMatRgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(boxMatRgb, displayImage);
            displayImageView.setImageBitmap(displayImage);
        }
        catch (CvException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    Point processImages(Mat box, Mat piece) {

        // Initialize OpenCV Items
        detector = FeatureDetector.create(FeatureDetector.ORB);
        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        double bestScore = 0;
        Point bestPosition = new Point(-1,-1);  //indexed from top left

        for (int divisions = 1; divisions <= MAX_DIVISIONS; divisions++) {
            Log.i(TAG, "Attempting " + divisions + " divisions");

            // for number of quadrants
            for (int quad = 0; quad < divisions * divisions; quad++) {
                Log.i(TAG, "Attempting quadrant " + quad);

                //      divide into quadrant
                Mat currQuad = getQuadrant(box, quad, divisions);
                //      get score for quadrant
                Pair<Double, Point> quadrantInfo = findMatches(currQuad, piece, divisions, quad);
                double score = quadrantInfo.first;
                if (score > bestScore) {
                    bestScore = score;
                    bestPosition = translatePosition(quadrantInfo.second, divisions, quad, box);
                }
            }
            // compare scores
            //      if one sufficiently high score, break
            //      otherwise, calculate and save position and score

            if (bestScore >= SCORE_THRESHOLD) {
                Log.i(TAG, "Score matches best score, exiting");
                break;
            }
        }
        Log.i(TAG, "Final Score: " + bestScore);
        Log.i(TAG, "Final Position: " + bestPosition.x + ", " + bestPosition.y);
        return bestPosition;

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
