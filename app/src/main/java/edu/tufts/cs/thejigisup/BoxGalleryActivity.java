package edu.tufts.cs.thejigisup;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("deprecation")
public class BoxGalleryActivity extends Activity {
    String currentBoxPath;

    private String[] getPictureList() {
        String path = Environment.getExternalStorageDirectory().toString() + "/jig/box";
        Log.d("Files","Path: "+path);
        File f = new File(path);
        File images[] = f.listFiles();
        if (images.length == 0) {
            return null;
        }
        Log.d("Files","Size: "+images.length);
        ArrayList<String> pictureList = new ArrayList<String>();
        for(int i = 0; i < images.length;i++)
        {
            Log.d("Files", "FileName:" + images[i].getName());
            pictureList.add(i, images[i].getAbsolutePath());
            Log.d("Images", "File:" + pictureList.get(i));
        }
        Collections.sort(pictureList);
        Collections.reverse(pictureList);
        String[] toReturn = new String[pictureList.size()];
        return pictureList.toArray(toReturn);
    }

    //the images to display
    String[] imageIDs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_gallery);

        final Activity baseContext = this;

        imageIDs = getPictureList();

        if (imageIDs == null) {
            Log.d("jig::BoxGallery","No images, going to box taking picture");
            Intent intent = new Intent(getBaseContext(), PhotoActivity.class);
            intent.putExtra("activityMode", "PUZZLE_BOX");
            Toast toast = Toast.makeText(getApplicationContext(), "No Boxes Saved", Toast.LENGTH_LONG);
            toast.show();
            startActivity(intent);
            return;
        }

        GridView grid = (GridView) findViewById(R.id.grid1);
        grid.setAdapter(new ImageAdapter(this));
        grid.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position,long id)
            {
                // display the images selected
                currentBoxPath = imageIDs[position];
                Intent intent;
                intent = new Intent(getBaseContext(), PhotoActivity.class);
                intent.putExtra("activityMode", "PUZZLE_PIECES");
                intent.putExtra("boxImage", currentBoxPath);
                startActivity(intent);
            }
        });


        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position,long id) {

                Log.d("jig::boxGallery", "image view was long clicked.");
                currentBoxPath = imageIDs[position];
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                File toDelete = new File(currentBoxPath);
                                boolean isDeleted = toDelete.delete();
                                Intent intent;
                                intent = new Intent(getBaseContext(), BoxGalleryActivity.class);
                                startActivity(intent);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(baseContext);
                builder.setMessage("Are you sure you want to delete this box image?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

                return true;

            }
        });
    }

    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private int itemBackground;
        public ImageAdapter(Context c)
        {
            context = c;
            // sets a grey background; wraps around the images
            TypedArray a =obtainStyledAttributes(R.styleable.MyGallery);
            itemBackground = a.getResourceId(R.styleable.MyGallery_android_galleryItemBackground, 0);
            a.recycle();
        }
        // returns the number of images
        public int getCount() {
            return imageIDs.length;
        }
        // returns the ID of an item
        public Object getItem(int position) {
            return position;
        }
        // returns the ID of an item
        public long getItemId(int position) {
            return position;
        }
        // returns an ImageView view
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(context);
            imageView.setImageBitmap(decodeSampledBitmapFromFile(new File(imageIDs[position]).toString(), 200, 200));
            imageView.setImageURI(Uri.parse(new File(imageIDs[position]).toString()));
            imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setBackgroundResource(itemBackground);
            return imageView;
        }
    }
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    public static Bitmap decodeSampledBitmapFromFile(String path,int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }
}
