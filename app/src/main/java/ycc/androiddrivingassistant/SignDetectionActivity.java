package ycc.androiddrivingassistant;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.*;

import ycc.androiddrivingassistant.ui.ScreenInterface;


public class SignDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, ScreenInterface {

    private static final String TAG = "SignDetectionActivity";
    TextRecognizer textRecognizer;
    JavaCameraView javaCameraView;
    ImageView signImageView;
    Mat mGray, imgCopy;
    Mat circles;
    Mat signCopy;
    Rect signRegion;
    Bitmap bm;
    Boolean newSignFlag = false;
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

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            for (android.util.Size size : map.getOutputSizes(SurfaceTexture.class)) {
                float ratio = (float)size.getWidth() / (float)size.getHeight();
                if (ratio >= 1.3 && size.getWidth() < 900) {
                    imgHeight = size.getHeight();
                    imgWidth = size.getWidth();
                    break;
                }
            }
        }catch (Error error) {
            Log.e(TAG, "onCreate: ", error);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        textRecognizer = new TextRecognizer.Builder(this).build();
        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available.");

            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this,"Low Storage: Speed Limit detection will not work.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Low Storage");
            }
        }


        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

//        javaCameraView.enableFpsMeter();
        javaCameraView.setMaxFrameSize(imgWidth, imgHeight);

        signImageView = (ImageView) findViewById(R.id.sign_image_view);
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
        mGray  = new Mat();
        imgCopy = new Mat();
        circles = new Mat();
        signCopy = new Mat();
        mRed = new Mat();
        mGreen = new Mat();
        mBlue = new Mat();
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
        mGray = inputFrame.gray();
        Imgproc.blur(mGray, mGray, new Size(5, 5), new Point(2, 2));

        splitRGBChannels(inputFrame.rgba());
        applyThreshold();
        Imgproc.erode(mRed, mRed, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
        Imgproc.dilate(mRed, mRed, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        Imgproc.HoughCircles(mGray, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 2000, 175, 120, 20, 100);
//        Imgproc.cvtColor(mRed, mRed, Imgproc.COLOR_GRAY2BGR);

        if (circles.cols() > 0) {
            for (int x=0; x < Math.min(circles.cols(), 5); x++ ) {
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = 1;
                radius = (int) circleVec[2];

                int val = (radius*2) + 20;
                // defines the ROI
                signRegion = new Rect((int) (center.x - radius - 10), (int) (center.y - radius - 10), val, val);

                if (!newSignFlag) {
                    analyzeObject(inputFrame.rgba(), signRegion, radius);
                }
//                Log.i(TAG, "onCreate: " + Math.abs(radius*2));
            }
        }

        circles.release();
        signCopy.release();
        return inputFrame.rgba();
    }

    Mat mRed, mGreen, mBlue;

    public void splitRGBChannels(Mat rgb_split) {
        List<Mat> rgbChannels = new ArrayList<>();

        Core.split(rgb_split, rgbChannels);

        rgbChannels.get(0).copyTo(mRed);
        rgbChannels.get(1).copyTo(mGreen);
        rgbChannels.get(2).copyTo(mBlue);

        for (int i = 0; i < rgbChannels.size(); i++) {
            rgbChannels.get(i).release();
        }
    }

    public void applyThreshold() {
        Core.inRange(mRed, new Scalar(175), new Scalar(255), mRed);
        Core.inRange(mGreen, new Scalar(0), new Scalar(150), mGreen);
        Core.bitwise_and(mRed, mGreen, mRed);
    }

    String signValue = "";
    Boolean isRunning = false;

    public void analyzeObject(final Mat img, final Rect roi, final int radius) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                isRunning = true;
                Mat copy;
                try {
                    copy = new Mat(img, roi);
                    // Creates a bitmap with size of detected circle and stores the Mat into it
                    bm = Bitmap.createBitmap(Math.abs((radius * 2) + 20), Math.abs((radius * 2) + 20), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(copy, bm);
                } catch (Exception e) {
                    bm = null;
                    Log.e(TAG, "run: ", e);
                }

                if (bm != null) {
                    Frame imageFrame = new Frame.Builder().setBitmap(bm).build();
                    SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

                    for (int i = 0; i < textBlocks.size(); i++) {
                        TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

                        if (!signValue.equals(textBlock.getValue())) {
                            signValue = textBlock.getValue();
                            setUISign(signValue);
                        }
                    }
                }
                isRunning = false;
            }
        };

        if (!isRunning) {
            Thread textDetectionThread = new Thread(runnable);
            textDetectionThread.run();
        }
    }

    public void setUISign(String val) {
        uiRunnable.setSignImageView(signImageView);

        if (val.contains("60")) {
            uiRunnable.setSignVal(60);
        } else if (val.contains("80")) {
            uiRunnable.setSignVal(80);
        } else if (val.contains("100")) {
            uiRunnable.setSignVal(100);
        } else if (val.contains("50")) {
            uiRunnable.setSignVal(50);
        } else if (val.contains("120")) {
            uiRunnable.setSignVal(120);
        } else if (val.contains("30")) {
            uiRunnable.setSignVal(30);
        } else {
        }
        runOnUiThread(uiRunnable);
    }

    UiRunnable uiRunnable = new UiRunnable();

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
