package org.kernby.portraitcamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CameraActivity extends Activity implements View.OnClickListener {
    public static final String TAG = CameraActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 333;

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private Button mButtonCapture;
    private FrameLayout flCameraPreview;
    private Button mButton_new;
    private ImageView previewImage;
    private boolean cameraFront = false;
    private Camera.PictureCallback mPicture;
    private Bitmap bitmap;
    String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };

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

        cameraFront = true;
        previewImage = findViewById(R.id.preview_image);
        mButtonCapture = findViewById(R.id.button_capture);
        mButtonCapture.setOnClickListener(this);
        mButton_new = findViewById(R.id.new_btn);
        mButton_new.setOnClickListener(this);

        if(checkPermission()){
            chooseCamera();
        }else{
           requestPermissions();
        }
    }
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (result == PackageManager.PERMISSION_GRANTED && camera == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermissions(){
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
    }
    private int findFrontFacingCamera() {

        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;

    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;

            }

        }
        return cameraId;
    }
    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mCameraPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview
                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mCameraPreview.refreshCamera(mCamera);
            }
        }
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_capture:
                mCamera.takePicture(null, null, mPicture);
                break;
            case R.id.new_btn:
                retakeImage();

                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }
    private void retakeImage() {
        releaseCamera();
        cameraFront = true;
        chooseCamera();
        mCameraPreview.setVisibility(View.VISIBLE);
//        previewImage.setVisibility(View.GONE);
    }
    public void onResume() {

        super.onResume();
        if (mCamera == null) {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            mPicture = getPictureCallback();
            mCameraPreview.refreshCamera(mCamera);
            Log.d("nu", "null");
        } else {
            Log.d("nu", "no null");
        }

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
            Bitmap adjustedBitmap = null;
            try {
                pictureBitmap = new BitmapWorkerTask().execute(data).get();

                // To get the uri of the saved image
                Uri uri = bitmapToUriConverter(pictureBitmap);

                // make rotation by 90 degress as default is landscape
                Matrix matrix = new Matrix();
                matrix.preRotate(90);
                adjustedBitmap = Bitmap.createBitmap(pictureBitmap, 0, 0, pictureBitmap.getWidth(), pictureBitmap.getHeight()
                        , matrix, true);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
            } catch (ExecutionException e) {
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
            }


            previewImage.setImageBitmap(adjustedBitmap);
            flCameraPreview.setVisibility(View.INVISIBLE);
        }
    };

    //	private static int exifToDegrees(int exifOrientation) {
//		if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
//			return 90;
//		} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
//			return 180;
//		} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
//			return 270;
//		}
//		return 0;
//	}
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
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            private Bitmap adjustedBitmap;

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                try {
                    Bitmap pictureBitmap = new BitmapWorkerTask().execute(data).get();

                    // To get the uri of the saved image
                    Uri uri = bitmapToUriConverter(pictureBitmap);

                    // make rotation by 90 degress as default is landscape
                    Matrix matrix = new Matrix();
                    matrix.preRotate(90);
                    adjustedBitmap = Bitmap.createBitmap(pictureBitmap, 0, 0, pictureBitmap.getWidth(), pictureBitmap.getHeight()
                            , matrix, true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG, e.getMessage());
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    Log.d(TAG, e.getMessage());
                }

                previewImage.setImageBitmap(adjustedBitmap);
                flCameraPreview.setVisibility(View.GONE);

//
            }
        };
        return picture;
    }
    public Uri bitmapToUriConverter(Bitmap mBitmap) {
        Uri uri = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, 100, 100);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            Bitmap newBitmap = Bitmap.createScaledBitmap(mBitmap, 200, 200,
                    true);
            File file = new File(getFilesDir(), "Image"
                    + new Random().nextInt() + ".jpeg");
            FileOutputStream out = openFileOutput(file.getName(),
                    Context.MODE_WORLD_READABLE);
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            //get absolute path
            String realPath = file.getAbsolutePath();
            File f = new File(realPath);
            uri = Uri.fromFile(f);

        } catch (Exception e) {
            Log.e("Your Error Message", e.getMessage());
        }
        return uri;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                    chooseCamera();
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
//                    AppUtils.showToast(EditTreeScreen.this, "Permissions required to access files.");
                }
                break;
        }
    }
}
