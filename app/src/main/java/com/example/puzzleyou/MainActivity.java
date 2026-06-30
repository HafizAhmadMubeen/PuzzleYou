package com.example.puzzleyou;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    Button btnCamera, btnGallery;
    Uri photoUri;

    ActivityResultLauncher<Uri> cameraLauncher;
    ActivityResultLauncher<String> galleryLauncher;
    ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);

        setupLaunchers();

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void launchCamera() {
        photoUri = createImageUri();
        cameraLauncher.launch(photoUri);
    }

    private Uri createImageUri() {
        File imagesDir = new File(getCacheDir(), "images");
        imagesDir.mkdirs();
        File image = new File(imagesDir, "camera_photo.jpg");
        return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", image);
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        openPuzzleActivity(photoUri);
                    } else {
                        Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        openPuzzleActivity(uri);
                    } else {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) launchCamera();
                    else Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show();
                });
    }

    // Save the picked/captured image to a fixed cache file, then pass its PATH to PuzzleActivity.
    // (We pass a file path string via Intent, not the Bitmap itself - Bitmaps are too big for Intents)
    private void openPuzzleActivity(Uri sourceUri) {
        try {
            Bitmap bitmap = loadBitmapFromUri(sourceUri);
            if (bitmap == null) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(getCacheDir(), "puzzle_source.jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            out.close();

            Intent intent = new Intent(MainActivity.this, PuzzleActivity.class);
            intent.putExtra("imagePath", file.getAbsolutePath());
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing puzzle", Toast.LENGTH_SHORT).show();
        }
    }

    // Loads image capped at 1200px max dimension - sharp but safe on memory
    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            InputStream is1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is1, null, boundsOptions);
            is1.close();

            int maxDimension = 1200;
            int sampleSize = 1;
            while (boundsOptions.outWidth / sampleSize > maxDimension
                    || boundsOptions.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2;
            }

            BitmapFactory.Options finalOptions = new BitmapFactory.Options();
            finalOptions.inSampleSize = sampleSize;
            InputStream is2 = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is2, null, finalOptions);
            is2.close();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}