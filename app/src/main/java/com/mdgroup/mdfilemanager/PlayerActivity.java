package com.mdgroup.mdfilemanager;

import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import com.squareup.picasso.Picasso;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

public class PlayerActivity extends AppCompatActivity {

    private String uriString;
    private Uri uri;

    private MediaPlayer mediaPlayer;
    private SeekBar audioSeekBar;
    private TextView artistAudioTextView;
    private TextView titleAudioTextView;
    private TextView progressAudioTextView;
    private TextView durationAudioTextView;
    private ImageView audioImageView;
    private AudioTask audioTask;
    private SimpleDateFormat sdf1 = new SimpleDateFormat("mm:ss");
    private boolean PLAY;
    private int progressMillis;
    private int durationMillis;
    private int durationSec;
    private int progressSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MainActivity.TAG, "PlayerActivity onCreate");

        uriString = getIntent().getStringExtra("uri");
        uri = Uri.parse(uriString);
        String type = getIntent().getStringExtra("type");

        switch (type) {
            case "image":
                Log.d(MainActivity.TAG, "PlayerActivity image");
                setContentView(R.layout.player_image);
                ImageView playerImageView = findViewById(R.id.playerImageView);
                File myImageFile = new File(uriString);
                Picasso.get().load(myImageFile).into(playerImageView);
                //playerImageView.setImageURI(uri);
                //playerImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                playerImageView.setBackgroundColor(Color.BLACK);
                break;

            case "text":
                Log.d(MainActivity.TAG, "PlayerActivity text");
                setContentView(R.layout.player_text);
                TextView textTextView = findViewById(R.id.textTextView);
                File file = new File(uriString);
                StringBuilder text = new StringBuilder();
                try {
                    final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "Cp1251"));
                    String line;

                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                textTextView.setText(text.toString());
                break;

            case "video":
                Log.d(MainActivity.TAG, "PlayerActivity video");
                setContentView(R.layout.player_video);

                VideoView videoView = (VideoView) findViewById(R.id.videoView);
                videoView.setVideoURI(uri);
                videoView.setMediaController(new MediaController(this));
                videoView.requestFocus(0);
                videoView.setKeepScreenOn(true);
                videoView.start();

                break;

            case "audio":
                Log.d(MainActivity.TAG, "PlayerActivity audio");
                setContentView(R.layout.player_audio);
                PLAY = false;
                progressMillis = 0;

                audioSeekBar = findViewById(R.id.audioSeekBar);
                titleAudioTextView = findViewById(R.id.titleAudioTextView);
                artistAudioTextView = findViewById(R.id.artistAudioTextView);
                progressAudioTextView = findViewById(R.id.progressAudioTextView);
                durationAudioTextView = findViewById(R.id.durationAudioTextView);
                audioImageView = findViewById(R.id.audioImageView);

                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, uri);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                String audioTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String audioArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                durationMillis = Integer.parseInt(durationStr);
                artistAudioTextView.setText(audioArtist);
                titleAudioTextView.setText(audioTitle);
                durationAudioTextView.setText(sdf1.format(durationMillis));

                audioImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (PLAY) {
                            if (audioTask != null) {
                                audioTask.cancel(true);
                                PLAY = false;
                            }
                        } else {
                            audioTask = new AudioTask();
                            audioTask.execute(durationMillis, progressMillis);
                            PLAY = true;
                        }

                    }
                });

                audioTask = new AudioTask();
                audioTask.execute(durationMillis, progressMillis);
                PLAY = true;
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        if (audioTask != null) {
            audioTask.cancel(true);
        }
    }

    class AudioTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            audioImageView.setImageDrawable(getResources().getDrawable(R.drawable.baseline_pause_circle_outline_white_36dp));
            audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    progressAudioTextView.setText(sdf1.format(progress * 1000));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    progressSec = seekBar.getProgress();
                    progressMillis = progressSec * 1000;
                    mediaPlayer.seekTo(progressMillis);
                    progressAudioTextView.setText(sdf1.format(progressSec * 1000));
                }
            });
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            durationSec = (int) Math.floor(integers[0] / 1000);
            progressSec = (int) Math.floor(integers[1] / 1000);
            audioSeekBar.setMax(durationSec);
            audioSeekBar.setProgress(progressSec);

            mediaPlayer = MediaPlayer.create(PlayerActivity.this, uri);
            mediaPlayer.start();
            mediaPlayer.seekTo(integers[1]);

            for (progressSec = (int) Math.floor(integers[1] / 1000); progressSec < durationSec; progressSec++) {

                try {
                    if (isCancelled()) return null;
                    Thread.sleep(1000);
                    audioSeekBar.setProgress(progressSec + 1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(MainActivity.TAG, "exception = " + e.toString());
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mediaPlayer != null) {
                progressMillis = mediaPlayer.getCurrentPosition();
                mediaPlayer.stop();
            }
            audioImageView.setImageDrawable(getResources().getDrawable(R.drawable.baseline_play_circle_outline_white_36dp));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            PLAY = false;
            progressMillis = 0;
            audioImageView.setImageDrawable(getResources().getDrawable(R.drawable.baseline_play_circle_outline_white_36dp));
        }
    }
}

