package ycc.androiddrivingassistant;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.squareup.leakcanary.LeakCanary;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Random;

import ycc.androiddrivingassistant.ui.ScreenInterface;

import android.hardware.camera2.*;

public class LaneDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, ScreenInterface {

    private static final String TAG = "LaneDetectionActivity";
    JavaCameraView javaCameraView;
    Mat rgba, mGray, mCopy, mEdges;
    Mat outY, outW, out, white, yellow, hsv;
    Rect roi;
    int imgWidth=1920, imgHeight=1080;
    private Mat mIntermediateMat;

    int rows, cols, left, width;
    double top;

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
        //Check if permission is already granted
        //thisActivity is your activity. (e.g.: MainActivity.this)
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Give first an explanation, if needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            for (android.util.Size size : map.getOutputSizes(SurfaceTexture.class)) {
                Log.i(TAG, "imageDimension " + size);
                float ratio = (float)size.getWidth() / (float)size.getHeight();
                if (ratio >= 1.3 && size.getWidth() < 900) {
                    imgHeight = size.getHeight();
                    imgWidth = size.getWidth();
                    break;
                }
                Log.i(TAG, "ratio: " + ratio);
            }
        }catch (Error error) {
            Log.e(TAG, "onCreate: ", error);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_lane_detection);

        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

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
    public void onCameraViewStarted(int w, int h)
    {
        rows = h;
        cols = w;
        left = rows / 5;
        width = cols - left;
        top = rows / 2.5;

        mIntermediateMat = new Mat();
        outY = new Mat();
        outW = new Mat();
        out = new Mat();
        yellow = new Mat();
        white = new Mat();
        hsv = new Mat();
    }

    @Override
    public void onCameraViewStopped()
    {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }


    private Scalar lowYellow = new Scalar(0, 100, 100), highYellow = new Scalar(50, 255, 255);
    private Scalar lowWhite = new Scalar(20, 0, 180), highWhite = new Scalar(255, 80, 255);
    private Size blurVal = new Size(5, 5);
    private Point blurPt = new Point(2, 2);

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //System.gc();

        rgba = inputFrame.rgba();
        Imgproc.blur(inputFrame.rgba(), inputFrame.rgba(), blurVal, blurPt);

        Mat rgbaInnerWindow;
        Mat lines = new Mat();
        // rgbaInnerWindow & mIntermediateMat = ROI Mats
        rgbaInnerWindow = rgba.submat((int)top, rows, left, width);

        Imgproc.cvtColor(rgbaInnerWindow, hsv, Imgproc.COLOR_RGB2HSV);

        Core.inRange(hsv, lowYellow, highYellow, yellow);
        Core.inRange(hsv, lowWhite, highWhite, white);
        Core.bitwise_and(rgbaInnerWindow, rgbaInnerWindow, outY, yellow);
        Core.bitwise_and(rgbaInnerWindow, rgbaInnerWindow, outW, white);
        Core.add(outW, outY, out);

        white.release();
        yellow.release();
        outW.release();
        outY.release();

        Imgproc.Canny(rgbaInnerWindow, out, 75, 150);
        Imgproc.cvtColor(out, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
        Imgproc.HoughLinesP(out, lines, 1, Math.PI/180, 50, 50, 50);

        for (int i=0; i<lines.rows(); i++) {
            double[] points = lines.get(i, 0);
            double x1, y1, x2, y2;

            try {
                x1 = points[0];
                y1 = points[1];
                x2 = points[2];
                y2 = points[3];

                Point p1 = new Point(x1, y1);
                Point p2 = new Point(x2, y2);

                float slope = (float)(p2.y - p1.y) / (float)(p2.x - p1.x);
                if (slope > 0.5 && slope < 2 ) {
                    Imgproc.line(rgbaInnerWindow, p1, p2, new Scalar(0, 255, 0), 2);
                } else if (slope > -2 && slope < -0.5) {
                    Imgproc.line(rgbaInnerWindow, p1, p2, new Scalar(0, 255, 0), 2);
                }

            } catch (Error e) {
                Log.e(TAG, "onCameraFrame: ", e);
            }
        }

        Point pt1 = new Point(250, 20);
        Point pt2 = new Point(out.size().width - 250, 20);
        Point pt3 = new Point(50, out.size().height-25);
        Point pt4 = new Point(out.size().width-50, out.size().height-25);

        Imgproc.circle(rgbaInnerWindow, pt1, 2, new Scalar(255, 0, 0), 5);
        Imgproc.circle(rgbaInnerWindow, pt2, 2, new Scalar(255, 0, 0), 5);
        Imgproc.circle(rgbaInnerWindow, pt3, 2, new Scalar(255, 0, 0), 5);
        Imgproc.circle(rgbaInnerWindow, pt4, 2, new Scalar(255, 0, 0), 5);

        MatOfPoint2f src = new MatOfPoint2f(
                pt1, pt2, pt3, pt4);

        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0, 0),
                new Point(600, 0),
                new Point(0, 900),
                new Point(600, 900));

//        Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

//        Mat dstImage = new Mat();

//        Imgproc.warpPerspective(rgbaInnerWindow, dstImage, warpMat, new Size(600, 900));
//        Imgproc.warpPerspective(rgbaInnerWindow, dstImage, warpMat, new Size(600, 900), Imgproc.CV_WARP_INVERSE_MAP);

//        bmp = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(dstImage, bmp);

        rgbaInnerWindow.release();

        return rgba;
    }


    public void  onClickBtn(View v) {
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt();
        try {
            FileOutputStream fout = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/image" + n + ".png");
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fout);
            bmp.recycle();
            fout.flush();
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
