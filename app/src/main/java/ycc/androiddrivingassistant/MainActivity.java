package ycc.androiddrivingassistant;


import android.graphics.Bitmap;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import ycc.androiddrivingassistant.ui.ScreenInterface;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, ScreenInterface {

    private static final String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    ImageView signImageView;
    Mat mGray, imgCopy;
    Mat circles;
    Mat signCopy;
    Rect signRegion;
    Bitmap bm;
    int imgWidth=960, imgHeight=544;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setFullscreen();

        // Example of a call to a native method
		/*TextView tv = (TextView) findViewById(R.id.sample_text);
		tv.setText(stringFromJNI());*/

        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        javaCameraView.enableFpsMeter();
        javaCameraView.setMaxFrameSize(imgWidth, imgHeight);

        signImageView = (ImageView) findViewById(R.id.sign_image_view);

        Log.i(TAG, "onCreate: " + Thread.currentThread());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected  void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialize success");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.d(TAG, "OpenCV initialize failed");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
        setFullscreen();
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray  = new Mat(height, width, CvType.CV_8UC4);
        imgCopy = new Mat(height, width, CvType.CV_8UC4);
        circles = new Mat();
        signCopy = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        imgCopy.release();
        circles.release();
        signCopy.release();
    }

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc();

        mGray = inputFrame.gray();
        Imgproc.blur(mGray, mGray, new Size(5, 5), new Point(2, 2));
        Imgproc.HoughCircles(mGray, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 2000, 175, 120, 20, 100);

        if (circles.cols() > 0) {
            for (int x=0; x < Math.min(circles.cols(), 5); x++ ) {
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = 1;
                radius = (int) circleVec[2];

                // outlines detected circle
//				Imgproc.circle(mGray, center, 3, new Scalar(255, 255, 255), 3);
//				Imgproc.circle(mGray, center, radius, new Scalar(255, 255, 255), 2);

                int val = (radius*2) + 20;
                // defines the ROI
                signRegion = new Rect((int) (center.x - radius - 10), (int) (center.y - radius - 10), val, val);

                final int r = radius;

                // Set detected circle to ImageView using the Main UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {
                            // Copies ROI into new Mat
                            signCopy = new Mat(inputFrame.rgba(), signRegion);

                            // Creates a bitmap with size of detected circle and stores the Mat into it
                            bm = Bitmap.createBitmap(Math.abs((r * 2) + 20), Math.abs((r * 2) + 20), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(signCopy, bm);

                            signImageView.setImageBitmap(bm);
                        } catch (Exception e) {
                            Log.e(TAG, "onCreate: " + e);
                        }
                    }
                });

                Log.i(TAG, "onCreate: " + Math.abs(radius*2));
            }
        }

        circles.release();
        mGray.release();
        signCopy.release();
        return inputFrame.rgba();
    }

    public void setFullscreen() {
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
