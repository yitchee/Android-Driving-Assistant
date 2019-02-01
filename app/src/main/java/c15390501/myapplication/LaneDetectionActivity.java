package c15390501.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import c15390501.myapplication.ui.ScreenInterface;

public class LaneDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, ScreenInterface {

	private static final String TAG = "MainActivity";
	JavaCameraView javaCameraView;
	Mat mGray, imgCopy;
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

	// Used to load the 'native-lib' library on application startup.
	static
	{
		System.loadLibrary("native-lib");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
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
	public void onCameraViewStarted(int width, int height)
	{
		mGray  = new Mat(height, width, CvType.CV_8UC4);
		imgCopy = new Mat(height, width, CvType.CV_8UC4);
	}

	@Override
	public void onCameraViewStopped()
	{
		mGray.release();
		imgCopy.release();
	}

	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
	{
		mGray = inputFrame.gray();


		return mGray;
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
