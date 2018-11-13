package c15390501.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

	private static final String TAG = "MainActivity";
	JavaCameraView javaCameraView;
	Mat mRgba, imgGray, imgCanny;
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
		setContentView(R.layout.activity_main);

		this.getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

		// Example of a call to a native method
		/*TextView tv = (TextView) findViewById(R.id.sample_text);
		tv.setText(stringFromJNI());*/

		javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_veiw);
		javaCameraView.setVisibility(SurfaceView.VISIBLE);
		javaCameraView.setCvCameraViewListener(this);

		javaCameraView.enableFpsMeter();
		javaCameraView.setMaxFrameSize(960, 544);
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
	}
	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
	public native String stringFromJNI();

	@Override
	public void onCameraViewStarted(int width, int height) {
		mRgba  = new Mat(height, width, CvType.CV_8UC4);
		imgGray  = new Mat(height, width, CvType.CV_8UC1);
		imgCanny  = new Mat(height, width, CvType.CV_8UC1);
	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
	}

	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();

		Imgproc.cvtColor(mRgba, imgGray, Imgproc.COLOR_RGB2GRAY);
		Imgproc.Canny(imgGray, imgCanny, 80, 100);
		return imgCanny;
	}
}
