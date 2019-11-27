package com.example.mc_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.method.Touch;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.visual_recognition.v3.model.ClassifyOptions;
import com.ibm.watson.visual_recognition.v3.VisualRecognition;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ObjectActivity extends AppCompatActivity {

    private static final int PIC_REQUEST_ID = 111;
    public static Uri imageUri;
    public static String objectClass = "";
    public static TTS ttsObject;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    200);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    200);
        }
        ttsObject = new TTS();

    }
    public void startCapture(View view){
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera, PIC_REQUEST_ID);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == PIC_REQUEST_ID){
            if(resultCode == RESULT_OK) {
                if(data!=null){
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    imageUri = getImageUri(getApplicationContext(), photo);
                    if(imageUri !=null){

                    }
                    Log.d("Image URI: ", imageUri.toString());
                    File imageFile = new File(getRealPathFromURI(imageUri));
                    classifyImage(imageFile);
//                    TTSHelper.getInstance(this).speak("The object is " + objectClass);
                }
                Toast.makeText(ObjectActivity.this, "Data response is null", Toast.LENGTH_LONG);

            }
        }

    }
    public void classifyImage(File imageFile){
        ObjectClassify classification = new ObjectClassify();
        classification.execute(imageFile);
        Log.d("ObjectClass", objectClass);
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
    public String getRealPathFromURI(Uri uri) {
        String path = "";
        if (getContentResolver() != null) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                path = cursor.getString(idx);
                cursor.close();
            }
        }
        return path;
    }

    private class ObjectClassify extends AsyncTask<File, String, String> {
        public String classify(File imageFile){
            IamAuthenticator authenticator = new IamAuthenticator("GMEIU-KCqpTuqU4on2pRU5eLiWCLRHsp-emjHHA9wlbS");
            VisualRecognition visualRecognition = new VisualRecognition("2018-03-19", authenticator);
            visualRecognition.setServiceUrl("https://gateway.watsonplatform.net/visual-recognition/api");
            String output="";
            try{
                InputStream imageStream = new FileInputStream(imageFile);
                ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                        .imagesFile(imageStream).imagesFilename(imageFile.toString())
                        .classifierIds(Arrays.asList("default"))
                        .build();
                ClassifiedImages result = visualRecognition.classify(classifyOptions).execute().getResult();
                output = result.getImages().get(0).getClassifiers().get(0).getClasses().get(0).getXClass();
            }catch(FileNotFoundException ex){
                Toast.makeText(ObjectActivity.this, "Image File not found",
                        Toast.LENGTH_LONG);
                return "";
            }
            return output;
        }

        @Override
        protected String doInBackground(File... files) {
            return classify(files[0]);
        }

        @Override
        protected void onPostExecute(String objectClass){
            ObjectActivity.objectClass = objectClass;
//            Intent ttsIntent = new Intent(ObjectActivity.this, TTSActivity.class);
//            ttsIntent.putExtra("ImageURI", ObjectActivity.imageUri);
//            ttsIntent.putExtra("ObjectClass", objectClass);
//            startActivity(ttsIntent);
            objectClass = "The object is "+ objectClass;
            ObjectActivity.ttsObject.textToSpeech(objectClass);
        }
    }

    private class TTS{
        private TextToSpeech textToSpeech;
        public TTS(){
            if(textToSpeech!=null){
                textToSpeech.stop();
            }
            textToSpeech = new TextToSpeech(getApplicationContext(), new android.speech.tts.TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                        int ttsLang = textToSpeech.setLanguage(Locale.US);

                        if (ttsLang == android.speech.tts.TextToSpeech.LANG_MISSING_DATA
                                || ttsLang == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TTS", "The Language is not supported!");
                        } else {
                            Log.i("TTS", "Language Supported.");
                        }
                        Log.i("TTS", "Initialization success.");
                    } else {
                        Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
        public void textToSpeech(String text){
            Log.d("TTS", "TTS Speak working");
            Log.d("TTS", "Text to speak is "+ text);
            int speechStatus = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);

            if (speechStatus == TextToSpeech.ERROR) {
                Log.e("TTS", "Error in converting Text to Speech!");
            }
        }
    }

}

