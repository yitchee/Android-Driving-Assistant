package ycc.androiddrivingassistant;


import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import ycc.androiddrivingassistant.ui.ScreenInterface;

public class LaneDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, ScreenInterface {

    private static final String TAG = "LaneDetectionActivity";
    JavaCameraView javaCameraView;
    Mat mGray, mCopy, mEdges;
    Rect roi;
    int imgWidth=960, imgHeight=544;
    private Mat mIntermediateMat;

    ImageView laneImageView;
    Bitmap bmp;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_lane_detection);

        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        laneImageView = (ImageView) findViewById(R.id.lane_view);

        javaCameraView.enableFpsMeter();
        javaCameraView.setMaxFrameSize(imgWidth, imgHeight);
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
    public void onCameraViewStarted(int width, int height)
    {
        mIntermediateMat = new Mat();
    }

    @Override
    public void onCameraViewStopped()
    {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();
        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;
        int left = rows / 5;
        int width = cols - left;
        double top = rows / 2.5;

        Bitmap bm;

        Mat rgbaInnerWindow;

        // rgbaInnerWindow & mIntermediateMat = ROI Mats
        rgbaInnerWindow = rgba.submat((int)top, rows, left, width);
        Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80, 90);
        Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);

//		bm = Bitmap.createBitmap(rgbaInnerWindow.width(), rgbaInnerWindow.height(), Bitmap.Config.ARGB_8888);
//		Utils.matToBitmap(rgbaInnerWindow, bm);\
        Point pt1 = new Point(0, 0);
        Point pt2 = new Point(mIntermediateMat.size().width, 0);
        Point pt3 = new Point(0, mIntermediateMat.size().height);
        Point pt4 = new Point(mIntermediateMat.size().width, mIntermediateMat.size().height);

        Mat mOriginal = new Mat();
        Log.i(TAG, "onCameraFrame: " + pt1 + "" + pt2 + "" + pt3 + "" + pt4);

        final Mat mPerspective = Imgproc.getPerspectiveTransform(rgbaInnerWindow, mOriginal);

        // Set detected circle to ImageView using the Main UI Thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    // Copies ROI into new Mat


                    // Creates a bitmap with size of detected circle and stores the Mat into it
                    bmp = Bitmap.createBitmap(mPerspective.width(), mPerspective.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mPerspective, bmp);

                    laneImageView.getLayoutParams().width = mPerspective.width();
                    laneImageView.getLayoutParams().height = mPerspective.height();
                    laneImageView.setImageBitmap(bmp);
                } catch (Exception e) {
                    Log.e(TAG, "onCreate: " + e);
                }
            }
        });
        //Imgproc.warpPerspective();

        rgbaInnerWindow.release();

        return rgba;
    }

    @Override
    public void setFullscreen()
    {
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
