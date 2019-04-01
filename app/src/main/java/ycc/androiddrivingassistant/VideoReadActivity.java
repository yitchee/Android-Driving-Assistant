package ycc.androiddrivingassistant;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ycc.androiddrivingassistant.ui.ScreenInterface;

public class VideoReadActivity extends AppCompatActivity implements ScreenInterface, SurfaceHolder.Callback, MediaPlayer.OnPreparedListener{
    private static final String TAG = "VideoReadActivity";
    ImageView imageView;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Button button;
    Mat frame;
    MediaMetadataRetriever mediaMetadataRetriever;
    MediaPlayer mediaPlayer;
    String VIDEO_PATH = "storage/emulated/0/Download/dashVideo2.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_read);

        imageView = findViewById(R.id.image_view);
        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(VIDEO_PATH);

        surfaceView = findViewById(R.id.surface_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(VideoReadActivity.this);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        setFullscreen();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialize success");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.d(TAG, "OpenCV initialize failed");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
    }

    @Override
    public void setFullscreen() {
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDisplay(surfaceHolder);
        try {
            mediaPlayer.setDataSource(VIDEO_PATH);
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(VideoReadActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            String root = Environment.getExternalStorageDirectory().toString();
            Bitmap bm;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//            int i = 0;
//            while (true) {
//                if (mediaPlayer.isPlaying()) {
//                    Log.i(TAG, "run: " + mediaPlayer.getCurrentPosition()*1000);
//                    bm = mediaMetadataRetriever.getFrameAtTime((long)(mediaPlayer.getCurrentPosition()*1000), MediaMetadataRetriever.OPTION_CLOSEST);
//                    String filename = "test" + i +".jpg";
//                    bm.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//
//                    try (FileOutputStream out = new FileOutputStream(root+"/AzRecorderFree/"+filename)) {
//                        bm.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
//                        // PNG is a lossless format, the compression factor (100) is ignored
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    i++;
////                    if (mediaPlayer.getCurrentPosition() > 2000) {
////                        runOnUiThread(uiRunnable);
////                    }
//                }
////                long time = System.currentTimeMillis();
////                while (System.currentTimeMillis()-time < 500) {
////
////                }
//            }
            int millis = mediaPlayer.getDuration();
            for(int i=1;i<millis/1000;i++) {
                Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(i*1000*1000, MediaMetadataRetriever.OPTION_NEXT_SYNC);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                String filename = "test" + i +".jpg";
                    try (FileOutputStream out = new FileOutputStream(root+"/AzRecorderFree/"+filename)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    });

    Runnable uiRunnable = new Runnable() {
        @Override
        public void run() {
            imageView.setImageBitmap(mediaMetadataRetriever.getFrameAtTime(mediaPlayer.getCurrentPosition()*1000, MediaMetadataRetriever.OPTION_CLOSEST));
        }
    };
    @Override
    public void onPrepared(MediaPlayer mp) {

        mediaPlayer.start();
        thread.start();
    }
}
