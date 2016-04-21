package edu.tufts.cs.thejigisup;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
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

        imageIDs = getPictureList();

        //Initialize the main view to the most recent image
        ImageView imageView = (ImageView) findViewById(R.id.image1);
        imageView.setImageURI(Uri.parse(new File(imageIDs[0]).toString()));

        // Note that Gallery view is deprecated in Android 4.1---
        Gallery gallery = (Gallery) findViewById(R.id.gallery1);
        gallery.setAdapter(new ImageAdapter(this));
        gallery.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position,long id)
            {
                Toast.makeText(getBaseContext(),"pic" + (position + 1) + " selected",
                        Toast.LENGTH_SHORT).show();
                // display the images selected
                currentBoxPath = imageIDs[position];
                ImageView imageView = (ImageView) findViewById(R.id.image1);
                imageView.setImageURI(Uri.parse(new File(currentBoxPath).toString()));
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("jig::boxGallery", "image view was clicked.");
                Intent intent;
                intent = new Intent(getBaseContext(), PhotoActivity.class);
                intent.putExtra("activityMode", "PUZZLE_PIECES");
                intent.putExtra("boxImage", currentBoxPath);
                startActivity(intent);

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
            imageView.setImageURI(Uri.parse(new File(imageIDs[position]).toString()));
            imageView.setLayoutParams(new Gallery.LayoutParams(100, 100));
            imageView.setBackgroundResource(itemBackground);
            return imageView;
        }
    }
}
