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

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java4");
    }

    private VideoCapture videoCapture;
    private ImageView imageView;
    private static final int REQUEST_CODE = 1;

    public static Bitmap convertMatToBitmap(Mat mat) {
        Bitmap ret = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, ret);
        return ret;
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
            videoCapture.open(0);
            Mat frame = new Mat();
            while (videoCapture.read(frame) && !frame.empty()) {
                Bitmap frameBitmap = convertMatToBitmap(frame);
                final BitmapDrawable frameDrawable = new BitmapDrawable(getResources(), frameBitmap);
                runOnUiThread(() -> imageView.setImageDrawable(frameDrawable));
            }
            videoCapture.release();
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