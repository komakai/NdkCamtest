package net.telepathix.ndkcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java4");
    }

    private final Object sync = new Object();
    private VideoCapture videoCapture;
    private ImageView imageView;
    private static final int REQUEST_CODE = 1;
    private boolean releasing = false;

    public static Bitmap convertMatToBitmap(Mat mat) {
        Bitmap ret = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, ret);
        return ret;
    }

    private void syncOpen(int index) {
        synchronized (sync) {
            videoCapture.open(index);
        }
    }

    private boolean syncRead(Mat mat) {
        boolean ret;
        synchronized (sync) {
            ret = videoCapture.read(mat);
        }
        return ret;
    }

    private void syncRelease() {
        synchronized (sync) {
            videoCapture.release();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (haveCameraPermission()) {
            runCameraLoop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoCapture != null && videoCapture.isOpened()) {
            videoCapture.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            runCameraLoop();
        }
    }

    private void runCameraLoop() {
        new Thread(() -> {
            videoCapture = new VideoCapture();
            releasing = false;
            syncOpen(0);
            videoCapture.set(Videoio.CAP_PROP_CONVERT_RGB, 1);
            Mat frame = new Mat();
            while (!releasing && syncRead(frame) && !frame.empty()) {
                Bitmap frameBitmap = convertMatToBitmap(frame);
                final BitmapDrawable frameDrawable = new BitmapDrawable(getResources(), frameBitmap);
                runOnUiThread(() -> imageView.setImageDrawable(frameDrawable));
            }
            syncRelease();
            videoCapture = null;
        }).start();
    }

    private boolean haveCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions(new String[] { Manifest.permission.CAMERA }, REQUEST_CODE);
            return false;
        }
    }
}