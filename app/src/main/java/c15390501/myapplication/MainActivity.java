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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

	private static final String TAG = "MainActivity";
	JavaCameraView javaCameraView;
	Mat mGray, imgCanny, imgHsv, imgCopy;
	Mat circles;
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
		setContentView(R.layout.activity_main);

		setFullscreen();

		// Example of a call to a native method
		/*TextView tv = (TextView) findViewById(R.id.sample_text);
		tv.setText(stringFromJNI());*/

		javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_veiw);
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
	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
	public native String stringFromJNI();

	@Override
	public void onCameraViewStarted(int width, int height) {
		mGray  = new Mat(height, width, CvType.CV_8UC4);
//		imgCanny  = new Mat(height, width, CvType.CV_8UC1);
//		imgHsv = new Mat(height, width, CvType.CV_8UC4);
		imgCopy = new Mat(height, width, CvType.CV_8UC4);
		circles = new Mat();
	}
	@Override
	public void onCameraViewStopped() {
		mGray.release();
//		imgCanny.release();
//		imgHsv.release();
		imgCopy.release();
		circles.release();

	}

	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		mGray = inputFrame.gray();
		Imgproc.blur(mGray, mGray, new Size(7, 7), new Point(2, 2));
		Imgproc.HoughCircles(mGray, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 350, 100, 90, 50, 250);
//		Imgproc.cvtColor(mRgba, imgHsv, Imgproc.COLOR_RGB2HSV);

		if (circles.cols() > 0) {
			for (int x=0; x < Math.min(circles.cols(), 5); x++ ) {
				double circleVec[] = circles.get(0, x);

				if (circleVec == null) {
					break;
				}

				Point center = new Point((int) circleVec[0], (int) circleVec[1]);
				int radius = (int) circleVec[2];

				Imgproc.circle(mGray, center, 3, new Scalar(255, 255, 255), 5);
				Imgproc.circle(mGray, center, radius, new Scalar(255, 255, 255), 2);
			}
		}

		circles.release();
		mGray.release();
		return inputFrame.rgba();
	}

	private void setFullscreen() {
		this.getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}
}
