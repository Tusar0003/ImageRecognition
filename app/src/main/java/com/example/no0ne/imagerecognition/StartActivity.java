package com.example.no0ne.imagerecognition;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.contract.Scores;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_CODE = 100;

    private Bitmap mBitmap;

    private ImageView mImageView;
    private Button mSelectImageButton;
    private Button mProcessImageButton;

    private EmotionServiceClient serviceClient = new EmotionServiceRestClient("a79a4cee1d2643099d52577eec7996b2");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mImageView = (ImageView) findViewById(R.id.image_view);
        mSelectImageButton = (Button) findViewById(R.id.button_select_image);
        mProcessImageButton = (Button) findViewById(R.id.button_process_image);

        mSelectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        mProcessImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processImage();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//***Change Here***
        startActivity(intent);
        finish();
        System.exit(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_IMAGE_CODE) {
            Uri imageUri = data.getData();
            InputStream inputStream = null;

            try {
                inputStream = getContentResolver().openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            mBitmap = BitmapFactory.decodeStream(inputStream);
            mImageView.setImageBitmap(mBitmap);
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SELECT_IMAGE_CODE);
    }

    private void processImage() {
        // Converting Image to Stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Creating AsyncTask to process data
        AsyncTask<InputStream, String, List<RecognizeResult>> processAsync = new AsyncTask<InputStream, String,
                List<RecognizeResult>>() {

            ProgressDialog dialog = new ProgressDialog(StartActivity.this);

            @Override
            protected void onPreExecute() {
                dialog.show();
            }

            @Override
            protected void onProgressUpdate(String... values) {
                dialog.setMessage(values[0]);
            }

            @Override
            protected void onPostExecute(List<RecognizeResult> recognizeResults) {
                dialog.dismiss();

                try {
                    for (RecognizeResult result : recognizeResults) {
                        String status = getEmotion(result);
                        mImageView.setImageBitmap(ImageHelper.drawRectOnBitmap(mBitmap, result.faceRectangle, status));
                        Toast.makeText(StartActivity.this, status, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e("EXCEPTION", e.toString());
                }
            }

            @Override
            protected List<RecognizeResult> doInBackground(InputStream... inputStreams) {
                publishProgress("Please wait...");

                List<RecognizeResult> result = null;
                try {
                    result = serviceClient.recognizeImage(inputStreams[0]);
                } catch (EmotionServiceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return result;
            }
        };

        processAsync.execute(inputStream);
    }

    private String getEmotion(RecognizeResult result) {
        List<Double> list = new ArrayList<>();
        Scores scores = result.scores;

        // Add all emotions to list
        list.add(scores.anger);
        list.add(scores.contempt);
        list.add(scores.disgust);
        list.add(scores.fear);
        list.add(scores.happiness);
        list.add(scores.neutral);
        list.add(scores.sadness);
        list.add(scores.surprise);

        // Sort List
        Collections.sort(list);

        // Get maximum value from list
        double maxValue = list.get(list.size() - 1);

        if (maxValue == scores.anger) {
            return "Anger";
        } else if (maxValue == scores.contempt) {
            return "Contempt";
        } else if (maxValue == scores.disgust) {
            return "disgust";
        } else if (maxValue == scores.fear) {
            return "Fear";
        } else if (maxValue == scores.happiness) {
            return "Happiness";
        } else if (maxValue == scores.neutral) {
            return "Neutral";
        } else if (maxValue == scores.sadness) {
            return "Sadness";
        } else if (maxValue == scores.surprise) {
            return "Surprise";
        } else {
            return "Can not detect!";
        }
    }
}
