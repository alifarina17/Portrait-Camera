package org.kernby.portraitcamera;

import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class CameraActivity extends Activity implements View.OnClickListener {
	public static final String TAG = CameraActivity.class.getSimpleName();
	
	private Camera mCamera;
	private CameraPreview mCameraPreview;
	private Button mButtonCapture;
	private FrameLayout flCameraPreview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_camera);
		mCamera = getCameraInstance();
		
		try {
			flCameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
			mCameraPreview = (CameraPreview) flCameraPreview.getChildAt(0);
			mCameraPreview.setCameraPreview(this, mCamera);
		} catch (Exception e) {
			releaseCamera();
		}

		mButtonCapture = (Button) findViewById(R.id.button_capture);
		mButtonCapture.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button_capture:
			mCamera.takePicture(null, null, mPictureCallback);
			break;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera();
	}

	@Override
	public void onBackPressed() {
		releaseCamera();
		super.onBackPressed();
	}

	PictureCallback mPictureCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Bitmap pictureBitmap = null;
			try {
				pictureBitmap = new BitmapWorkerTask().execute(data).get();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Log.d(TAG, e.getMessage());
			} catch (ExecutionException e) {
				e.printStackTrace();
				Log.d(TAG, e.getMessage());
			}
			
			//pictureBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

			ImageView previewImage = (ImageView) findViewById(R.id.preview_image);

			previewImage.setImageBitmap(pictureBitmap);
			flCameraPreview.setVisibility(View.INVISIBLE);
		}
	};

	public static Bitmap decodeSampledBitmapFromByteArray(byte[] data,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	class BitmapWorkerTask extends AsyncTask<byte[], Void, Bitmap> {
		private byte[] data;

		@Override
		protected Bitmap doInBackground(byte[]... bytes) {
			data = bytes[0];
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			bitmap = Bitmap
					.createScaledBitmap(bitmap, mCameraPreview.getWidth(),
							mCameraPreview.getHeight(), true);
			// return
			// decodeSampledBitmapFromByteArray(data,mCameraPreview.getWidth(),mCameraPreview.getHeight());
			return bitmap;
		}
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	public void releaseCamera() {
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

}
