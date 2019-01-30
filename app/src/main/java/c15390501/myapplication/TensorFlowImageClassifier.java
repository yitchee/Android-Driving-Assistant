package c15390501.myapplication;

import android.graphics.Bitmap;

import org.tensorflow.Graph;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.List;

public class TensorFlowImageClassifier implements Classifier {

	private static final String TAG = "TensorFlowImageClassifier";

	@Override
	public List<Recognition> recognizeImage(Bitmap bitmap) {
		return null;
	}

	@Override
	public void enableStatLogging(boolean debug) {

	}

	@Override
	public String getStatString() {
		return null;
	}

	@Override
	public void close() {

	}
}
