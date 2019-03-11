package ycc.androiddrivingassistant;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ycc.androiddrivingassistant.ui.ScreenInterface;

import android.hardware.camera2.*;

public class LaneDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, ScreenInterface {

    private static final String TAG = "LaneDetectionActivity";
    JavaCameraView javaCameraView;
    Mat mRgba, rgba, mGray, mCopy, mEdges;
    Mat outY, outW, out, hsv, hls;
    Rect roi;
    int imgWidth=1920, imgHeight=1080;
    private Mat mIntermediateMat;

    int rows, cols, left, width;
    double top, middleX, bottomY;

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

//        javaCameraView.enableFpsMeter();
        javaCameraView.setMaxFrameSize(imgWidth, imgHeight);

        Log.i(TAG, "ThreadInfo: " + Thread.currentThread());
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

    double vehicleCenterX1, vehicleCenterY1, vehicleCenterX2, vehicleCenterY2, laneCenterX, laneCenterY;

    @Override
    public void onCameraViewStarted(int w, int h)
    {
        rows = h;
        cols = w;
        left = rows / 8;
        width = cols - left;
        top = rows / 2.5;
        middleX = w /2;
        bottomY = h;

        vehicleCenterX1 = middleX;
        vehicleCenterX2 = middleX;
        vehicleCenterY1 = bottomY-(rows/7);
        vehicleCenterY2 = bottomY-(rows/20);
        laneCenterX = 0;
        laneCenterY = (bottomY-(rows/7) + bottomY-(rows/20)) / 2;

        mEdges = new Mat();
        mCombined = new Mat();
        mIntermediateMat = new Mat();
        outY = new Mat();
        outW = new Mat();
        out = new Mat();
        hsv = new Mat();
        hls = new Mat();
        gray = new Mat();
        mNew = new Mat();
        mask = new Mat();
        maskWhite = new Mat();
        maskYellow = new Mat();
        mGray = new Mat();
        mRgba = new Mat();
        rgba = new Mat();
        mRed = new Mat(); mGreen = new Mat(); mBlue = new Mat();
        mHue_hsv = new Mat(); mSat_hsv = new Mat(); mVal_hsv = new Mat();
        mHue_hls = new Mat(); mSat_hls = new Mat(); mLight_hls = new Mat();
    }

    @Override
    public void onCameraViewStopped()
    {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }


    private Size ksize = new Size(5, 5);
    private double sigma = 3;
    private Point blurPt = new Point(3, 3);

    Mat mNew, mask, maskWhite, maskYellow;
    Mat gray, mCombined;
    Mat mRed, mGreen, mBlue;
    Mat mHue_hsv, mSat_hsv, mVal_hsv;
    Mat mHue_hls, mSat_hls, mLight_hls;


    /******************************************************************************************
     * mRed, mGreen, mBlue, m-_hsv, m-_hls :  Mats of respective channels of ROI
     * mCombined : combined mat of canny edges and mask for yellow and white
     * hsv, hls, rgb : color space mats of ROI
     *
     ******************************************************************************************/
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //System.gc();

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Imgproc.GaussianBlur(mRgba, mRgba, ksize, sigma);

        Mat rgbaInnerWindow;
        Mat lines = new Mat();
        /* rgbaInnerWindow & mIntermediateMat = ROI Mats */
        rgbaInnerWindow = mRgba.submat((int)top, rows, left, width);
        rgbaInnerWindow.copyTo(rgba);
        Imgproc.cvtColor(rgbaInnerWindow, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(rgbaInnerWindow, hsv, Imgproc.COLOR_RGB2HSV);
        Imgproc.cvtColor(rgbaInnerWindow, hls, Imgproc.COLOR_RGB2HLS);

        splitRGBChannels(rgba, hsv, hls);
        applyThreshold();
        Imgproc.erode(mask, mask, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
        Imgproc.dilate(mask, mask, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        Imgproc.Canny(mask, mEdges, 50, 150);

        Imgproc.line(mRgba, new Point(vehicleCenterX1, vehicleCenterY1), new Point(vehicleCenterX2, vehicleCenterY2), new Scalar(0, 155, 0), 2, 8);
        Imgproc.HoughLinesP(mEdges, lines, 1, Math.PI/180, 50, 110, 50);
        if (lines.rows() > 0) {
            getAverageSlopes(lines);
        }

        Imgproc.resize(mask, mNew, new Size(imgWidth, imgHeight));
//        Point pt1 = new Point(250, 20);
//        Point pt2 = new Point(out.size().width - 250, 20);
//        Point pt3 = new Point(50, out.size().height-25);
//        Point pt4 = new Point(out.size().width-50, out.size().height-25);

//        Imgproc.circle(rgbaInnerWindow, pt1, 2, new Scalar(255, 0, 0), 5);
//        Imgproc.circle(rgbaInnerWindow, pt2, 2, new Scalar(255, 0, 0), 5);
//        Imgproc.circle(rgbaInnerWindow, pt3, 2, new Scalar(255, 0, 0), 5);
//        Imgproc.circle(rgbaInnerWindow, pt4, 2, new Scalar(255, 0, 0), 5);

//        MatOfPoint2f src = new MatOfPoint2f(
//                pt1, pt2, pt3, pt4);
//
//        MatOfPoint2f dst = new MatOfPoint2f(
//                new Point(0, 0),
//                new Point(600, 0),
//                new Point(0, 900),
//                new Point(600, 900));

//        Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

//        Mat dstImage = new Mat();

//        Imgproc.warpPerspective(rgbaInnerWindow, dstImage, warpMat, new Size(600, 900));
//        Imgproc.warpPerspective(rgbaInnerWindow, dstImage, warpMat, new Size(600, 900), Imgproc.CV_WARP_INVERSE_MAP);

//        bmp = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(dstImage, bmp);

        rgbaInnerWindow.release();
        Imgproc.rectangle(mRgba, new Point(left, top), new Point(imgWidth-left, rows), new Scalar(0, 255, 0), 2);
        return mRgba;
    }


    public void splitRGBChannels(Mat rgb_split, Mat hsv_split, Mat hls_split) {
        List<Mat> rgbChannels = new ArrayList<>();
        List<Mat> hsvChannels = new ArrayList<>();
        List<Mat> hlsChannels = new ArrayList<>();

        Core.split(rgb_split, rgbChannels);
        Core.split(hsv_split, hsvChannels);
        Core.split(hls_split, hlsChannels);

        rgbChannels.get(0).copyTo(mRed);
        rgbChannels.get(1).copyTo(mGreen);
        rgbChannels.get(2).copyTo(mBlue);

        hsvChannels.get(0).copyTo(mHue_hsv);
        hsvChannels.get(1).copyTo(mSat_hsv);
        hsvChannels.get(2).copyTo(mVal_hsv);

        hlsChannels.get(0).copyTo(mHue_hls);
        hlsChannels.get(1).copyTo(mSat_hls);
        hlsChannels.get(2).copyTo(mLight_hls);
//
//
        for (int i = 0; i < rgbChannels.size(); i++){
            rgbChannels.get(i).release();
        }

        for (int i = 0; i < hsvChannels.size(); i++){
            hsvChannels.get(i).release();
        }

        for (int i = 0; i < hlsChannels.size(); i++){
            hlsChannels.get(i).release();
        }
    }


    public void applyThreshold() {
        Core.inRange(mRed, new Scalar(210), new Scalar(255), mRed);
//        Core.inRange(mGreen, new Scalar(225), new Scalar(255), mGreen);
//        Core.inRange(mBlue, new Scalar(200), new Scalar(255), mBlue);

//        Core.inRange(mHue_hsv, new Scalar(200), new Scalar(255), mHue_hsv);
//        Core.inRange(mSat_hsv, new Scalar(200), new Scalar(255), mSat_hsv);
        Core.inRange(mVal_hsv, new Scalar(210), new Scalar(255), mVal_hsv);

//        Core.inRange(mHue_hls, new Scalar(200), new Scalar(255), mHue_hls);
//        Core.inRange(mLight_hls, new Scalar(200), new Scalar(255), mLight_hls);
//        Core.inRange(mSat_hls, new Scalar(200), new Scalar(255), mSat_hls);

        Core.bitwise_and(mRed, mVal_hsv, mask);
    }


    public void getAverageSlopes(Mat lines) {
        List<Double> left_slopes = new ArrayList<>();
        List<Double> right_slopes = new ArrayList<>();
        List<Double> left_y_intercept = new ArrayList<>();
        List<Double> right_y_intercept = new ArrayList<>();

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

                double slope = (p2.y - p1.y) / (p2.x - p1.x);
                double y_intercept = 0;

                if (slope > 0.5 && slope < 2 ) { // Right lane
                    right_slopes.add(slope);
                    y_intercept = p1.y - (p1.x*slope);
                    right_y_intercept.add(y_intercept);
                }
                else if (slope > -2 && slope < -0.5) { // Left lane
                    left_slopes.add(slope);
                    y_intercept = p1.y - (p1.x*slope);
                    left_y_intercept.add(y_intercept);
                }

            } catch (Error e) {
                Log.e(TAG, "onCameraFrame: ", e);
            }
        }

        double avg_left_slope = 0;
        double avg_right_slope = 0;
        double avg_left_y_intercept = 0;
        double avg_right_y_intercept = 0;

        for (int i=0; i< right_slopes.size(); i++) {
            avg_right_slope += right_slopes.get(i);
            avg_right_y_intercept += right_y_intercept.get(i);
        }
        avg_right_slope /= right_slopes.size();
        avg_right_y_intercept /= right_y_intercept.size();

        for (int i=0; i< left_slopes.size(); i++) {
            avg_left_slope += left_slopes.get(i);
            avg_left_y_intercept += left_y_intercept.get(i);
        }
        avg_left_slope /= left_slopes.size();
        avg_left_y_intercept /= left_y_intercept.size();

        //x = (y-b)/m
        //y = xm + b
        double newLeftTopX = (-avg_left_y_intercept)/avg_left_slope;
        double newRightTopX = (0 - avg_right_y_intercept)/avg_right_slope;

        Point rightLanePt = new Point((imgHeight - avg_right_y_intercept)/avg_right_slope, imgHeight);
        Point leftLanePt = new Point((0), (-left*avg_left_slope)+avg_left_y_intercept);
        Imgproc.putText(mRgba, "ROI Slope: " + avg_left_slope + " Other Slope: " + avg_left_y_intercept, new Point(0, 175), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 0, 0));

        if (left_slopes.size() != 0) {
            Imgproc.line(mRgba, new Point(newLeftTopX + left, 0 + top), new Point(leftLanePt.x, leftLanePt.y + top), new Scalar(0, 255, 255), 5);
        }
        if (right_slopes.size() != 0) {
            Imgproc.line(mRgba, new Point(rightLanePt.x + left, rightLanePt.y + top), new Point(newRightTopX + left, 0 + top), new Scalar(255, 0, 255), 5);
        }
        if (right_slopes.size() != 0 && left_slopes.size() != 0) {
            double laneCenterX1 = (laneCenterY-top-avg_left_y_intercept)/avg_left_slope + left;
            double laneCenterX2 = (laneCenterY-top-avg_right_y_intercept)/avg_right_slope + left;
            laneCenterX = (laneCenterX1+laneCenterX2) / 2;
            Imgproc.line(mRgba, new Point(vehicleCenterX1, laneCenterY), new Point(laneCenterX, laneCenterY), new Scalar(0, 155, 0), 2, 8);
            Imgproc.circle(mRgba, new Point(laneCenterX, laneCenterY), 4, new Scalar(0, 0, 255), 6);
        }
    }


    public void releaseAllMats() {
        mRed.release();
        mGreen.release();
        mBlue.release();
        mHue_hsv.release();
        mSat_hsv.release();
        mVal_hsv.release();
        mHue_hls.release();
        mLight_hls.release();
        mSat_hls.release();
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
