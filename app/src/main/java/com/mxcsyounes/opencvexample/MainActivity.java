package com.mxcsyounes.opencvexample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private final Matrix mMatrix = new Matrix();
    CascadeClassifier faceDetector;
    File casFile;
    private Mat mRgba, mGrey;
    private CameraBridgeViewBase cameraView;
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.d(TAG, "onManagerConnected: called succes");
                    InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    casFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
                    try {
                        FileOutputStream fos = new FileOutputStream(casFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        inputStream.close();
                        fos.close();
                        Log.d(TAG, "onManagerConnected: file" + casFile.getAbsolutePath());
                        faceDetector = new CascadeClassifier(casFile.getAbsolutePath());
                        if (faceDetector.empty()) {
                            faceDetector = null;
                        } else {
                            cascadeDir.delete();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private boolean isCameraBackMode = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.color_blob_detection_surface_view);

        cameraView = findViewById(R.id.color_blob_detection_activity_surface_view);

        checkPermissions();

        findViewById(R.id.switchButton).setOnClickListener(v -> {
            isCameraBackMode = !isCameraBackMode;
            selectCameraFromMode();
        });
    }

    private void selectCameraFromMode() {
        Log.d(TAG, "selectCameraFromMode: called with " +isCameraBackMode);
        cameraView.disableView();
        if (isCameraBackMode)
            cameraView.setCameraIndex(JavaCameraView.CAMERA_ID_BACK);
        else
            cameraView.setCameraIndex(JavaCameraView.CAMERA_ID_FRONT);
        cameraView.enableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    private void checkPermissions() {
        if (isPermissionGranted())
            loadCameraBridge();
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
    }

    private boolean isPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadCameraBridge() {
        cameraView.setVisibility(SurfaceView.VISIBLE);
        selectCameraFromMode();
        cameraView.setMaxFrameSize(500, 500);
        cameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(mRgba, faceDetections);
        for (Rect rect : faceDetections.toArray())
            Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));

        return mRgba;
    }
}