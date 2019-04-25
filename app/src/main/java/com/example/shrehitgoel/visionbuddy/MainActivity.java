package com.example.shrehitgoel.visionbuddy;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    ImageView imageView;
    Button readButton;
    Button cameraButton;
    TextView textView;
    TextToSpeech tts;
    Bitmap bitmap;
    private static final String FILENAME="CameraContentDemo.jpeg";
    private static final int CONTENT_REQUEST=1337;
    private static final String AUTHORITY=
            BuildConfig.APPLICATION_ID+".provider";
    private static final String PHOTOS="photos";
    private File output=null;
    private static final String[] PERMS ={
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    final String PIC_COUNTER = "Pic counter";
    boolean savedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        output=new File(new File(getFilesDir(), PHOTOS), FILENAME);
        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.text);
        textView.setMovementMethod(new ScrollingMovementMethod());
        readButton = findViewById(R.id.readButton);
        cameraButton = findViewById(R.id.camera_button);
        Button stopButton = findViewById(R.id.stop);

        savedImage = false;
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            new Thread(() -> {
                if (!savedImage && output.exists() && storagePermissionGranted())
                {
                    savedImage = true;
                    createExternalStoragePublicPicture();
                }
            }).start();
        });

        new Thread(() -> {
            bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.hello);
            runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            tts = new TextToSpeech(getApplicationContext(), this);
            if (output.exists()) {
                output.delete();
            }
            else {
                output.getParentFile().mkdirs();
            }
        }).start();

        cameraButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {

                if(!storagePermissionGranted())
                    return;

                new Thread(){

                    @Override
                    public void run() {
                        super.run();
                        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        Uri outputUri= FileProvider.getUriForFile(getApplicationContext(), AUTHORITY, output);
                        i.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

                        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        try {
                            startActivityForResult(i, CONTENT_REQUEST);
                        }
                        catch (ActivityNotFoundException e) {
                            Toast.makeText(getApplicationContext(), "msg_no_camera", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }.start();
            }
        });

        readButton.setOnClickListener(v -> {
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                    if (!textRecognizer.isOperational()) {
                        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                    } else {
                        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                        SparseArray<TextBlock> items = textRecognizer.detect(frame);
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < items.size(); i++) {
                            TextBlock item = items.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");
                        }
                        runOnUiThread(() -> {
                            textView.setText(stringBuilder.toString());
                            tts.speak(textView.getText(), TextToSpeech.QUEUE_FLUSH, null, null);
                        });
                    }
                }
            }.start();
        });
        stopButton.setOnClickListener((v) -> {
            tts.stop();
        });
    }

    void createExternalStoragePublicPicture() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int picCount = sharedPref.getInt(PIC_COUNTER, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PIC_COUNTER, picCount + 1);
        editor.apply();

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File file = new File(path, "Vision_" + picCount + ".jpg");

        try {
            // Make sure the Pictures directory exists.
            path.mkdirs();

            InputStream is = new FileInputStream(output);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });

            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Saved Image", Toast.LENGTH_LONG).show());
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }

    private  boolean storagePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestPermissions(PERMS, 1000);
                return false;
            }
        }
        else {
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
            cameraButton.performClick();
    }

    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR)
            tts.setLanguage(Locale.US);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Activity act = this;
        new Thread(){
            @Override
            public void run() {
                super.run();
                if (requestCode == CONTENT_REQUEST) {
                    if (resultCode == RESULT_OK) {
                        Uri outputUri=FileProvider.getUriForFile(act, AUTHORITY, output);
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(act.getContentResolver(), outputUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        act.runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                        savedImage = false;
                    }
                }
            }
        }.start();
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.stop();
        tts.shutdown();
    }
}
