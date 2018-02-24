package com.example.shrehitgoel.visionbuddy;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.FileProvider;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener{

    Activity thisActivity;
    ImageView imageView;
    Button button;
    Button cameraButton;
    TextView textView;
    TextToSpeech tts;
    Bitmap bitmap;
    private static final String EXTRA_FILENAME=
            "com.commonsware.android.camcon.EXTRA_FILENAME";
    private static final String FILENAME="CameraContentDemo.jpeg";
    private static final int CONTENT_REQUEST=1337;
    private static final String AUTHORITY=
            BuildConfig.APPLICATION_ID+".provider";
    private static final String PHOTOS="photos";
    private File output=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        output=new File(new File(getFilesDir(), PHOTOS), FILENAME);

        if (output.exists()) {
            output.delete();
        }
        else {
            output.getParentFile().mkdirs();
        }
        thisActivity = this;
        setContentView(R.layout.main);
        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.text);
        button = findViewById(R.id.button);
        cameraButton = findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
                Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri outputUri= FileProvider.getUriForFile(getApplicationContext(), AUTHORITY, output);
                i.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
                else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip=
                            ClipData.newUri(getContentResolver(), "A photo", outputUri);

                    i.setClipData(clip);
                    i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
                else {
                    List<ResolveInfo> resInfoList =
                            getPackageManager()
                                    .queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);

                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, outputUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                }
                try {
                    startActivityForResult(i, CONTENT_REQUEST);
                }
                catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "msg_no_camera", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
        bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.hello);
        imageView.setImageBitmap(bitmap);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                if(!textRecognizer.isOperational())
                {
                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                }
                else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = textRecognizer.detect(frame);
                    StringBuilder stringBuilder = new StringBuilder();
                    for(int i = 0; i< items.size();i++)
                    {
                        TextBlock item = items.valueAt(i);
                        stringBuilder.append(item.getValue());
                        stringBuilder.append("\n");
                    }
                    textView.setText(stringBuilder.toString());
                    tts = new TextToSpeech(getApplicationContext(), (TextToSpeech.OnInitListener)thisActivity);
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        tts.speak(textView.getText(), TextToSpeech.QUEUE_FLUSH, null,null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONTENT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Intent i=new Intent(Intent.ACTION_VIEW);
                Uri outputUri=FileProvider.getUriForFile(this, AUTHORITY, output);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), outputUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(bitmap);

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
