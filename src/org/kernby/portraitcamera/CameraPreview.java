package org.kernby.portraitcamera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.List;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {

	private static final String TAG = CameraPreview.class.getSimpleName();
	private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
	private static final int PREVIEW_SIZE_MAX_WIDTH = 640;

	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	private Context mContext;

	public CameraPreview(Context context) {
		super(context);
	}

	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setCameraPreview(Context context, Camera camera) {
		mContext = context;
		mCamera = camera;

		requestLayout();
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceHolder.setKeepScreenOn(true);
	}

	public void surfaceCreated(SurfaceHolder surfaceHolder) {
	}

	public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w,
			int h) {
		if (mSurfaceHolder.getSurface() == null) {
			return;
		}

		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			e.printStackTrace();
			// ignore: tried to stop a non-existent preview
		}

		try {
			int degress = getRotationDegrees();
			mCamera.setDisplayOrientation(degress);

			Camera.Parameters cameraParameters = mCamera.getParameters();
			cameraParameters.setJpegQuality(50);
			cameraParameters.setPictureFormat(PixelFormat.JPEG);
			cameraParameters.setRotation(90);

			Size bestPreviewSize = determineBestPreviewSize(cameraParameters);
			Size bestPictureSize = determineBestPictureSize(cameraParameters);

			cameraParameters.setPreviewSize(bestPreviewSize.width,
					bestPreviewSize.height);
			cameraParameters.setPictureSize(bestPictureSize.width,
					bestPictureSize.height);

			// cameraParameters.setPreviewSize(mPreviewSize.width,
			// mPreviewSize.height);
			requestLayout();
			mCamera.setParameters(cameraParameters);
			mCamera.setPreviewDisplay(mSurfaceHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Error setting camera preview" + e.getMessage());
			mCamera.release();
			mCamera = null;
		}
	}

	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		if (mCamera != null) {
			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				e.printStackTrace();
				// ignore: tried to stop a non-existent preview
			}
		}
	}

	private Size determineBestPreviewSize(Camera.Parameters parameters) {
		List<Size> sizes = parameters.getSupportedPreviewSizes();

		return determineBestSize(sizes, PREVIEW_SIZE_MAX_WIDTH);
	}

	private Size determineBestPictureSize(Camera.Parameters parameters) {
		List<Size> sizes = parameters.getSupportedPictureSizes();

		return determineBestSize(sizes, PICTURE_SIZE_MAX_WIDTH);
	}

	protected Size determineBestSize(List<Size> sizes, int widthThreshold) {
		Size bestSize = null;

		for (Size currentSize : sizes) {
			boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
			boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
			boolean isInBounds = currentSize.width <= PICTURE_SIZE_MAX_WIDTH;

			if (isDesiredRatio && isInBounds && isBetterSize) {
				bestSize = currentSize;
			}
		}

		if (bestSize == null) {

			return sizes.get(0);
		}

		return bestSize;
	}

	protected int getRotationDegrees() {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(0, info);
		int rotation = ((WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
				.getRotation();
		int degrees = 0, result = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		result = (info.orientation - degrees + 360) % 360;
		return result;
	}
}