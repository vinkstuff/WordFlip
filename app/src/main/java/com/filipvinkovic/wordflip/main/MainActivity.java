package com.filipvinkovic.wordflip.main;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.filipvinkovic.wordflip.R;
import com.filipvinkovic.wordflip.com.filipvinkovic.wordflip.database.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Filip on 26.8.2014..
 */
public class MainActivity extends Activity {
    private static final String LOG_STRING = "WordFlip";
    private static final int RECORDER_SAMPLERATE = 22050;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private String filePath = null;
    private String reversedFilePath = null;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private MediaPlayer mPlayer = null;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private int bufferSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final Database db = new Database(this);

        final TextView twWord = (TextView) findViewById(R.id.twWord);
        twWord.setText(db.getRandomWord());
        twWord.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                twWord.setText(db.getRandomWord());
            }
        });

        final Button recordButton = (Button) findViewById(R.id.btnRecord);
        recordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!isRecording) {
                    startRecording();
                    recordButton.setBackgroundResource(R.drawable.record_on_icon);
                }
                else {
                    stopRecording();
                    recordButton.setBackgroundResource(R.drawable.record_off_icon);
                }
            }
        });

        final ImageButton playButton = (ImageButton) findViewById(R.id.btnPlay);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!isPlaying) {
                    playAudio(false);
                }
            }
        });

        final ImageButton playReverseButton = (ImageButton) findViewById(R.id.btnPlayReverse);
        playReverseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!isPlaying) {
                    playAudio(true);
                }
            }
        });

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath = directoryPath + "/wfaudio.pcm";
        reversedFilePath = directoryPath + "/wfreversedaudio.pcm";
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize /*BufferElements2Rec * BytesPerElement*/);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            Log.e(LOG_STRING, e.getMessage());
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                Log.e(LOG_STRING, e.getMessage());
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            Log.e(LOG_STRING, e.getMessage());
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    private void playAudio(boolean reverse) {
        //Reading the file..
        byte[] byteData = null;
        byte[] reversedData = null;
        File file = null;
        file = new File(filePath);
        byteData = new byte[(int) file.length()];
        FileInputStream in = null;
        try {
            in = new FileInputStream( file );
            in.read( byteData );
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if(reverse) {
            reversedData = new byte[byteData.length];
            for(int i = 0; i < byteData.length; i += 2) {
                reversedData[i] = byteData[byteData.length - 2 - i];
                reversedData[i+1] = byteData[byteData.length - 1 - i];
            }
        }

        // Set and push to audio track..
        int intSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);
        if (at!=null) {
            at.play();
            // Write the byte array to the track
            if(reverse) {
                at.write(reversedData, 0, reversedData.length);
            } else
                at.write(byteData, 0, byteData.length);
            at.stop();
            at.release();
        }
        else
            Log.d(LOG_STRING, "audio track is not initialised ");
    }

    private void stopAudio() {
        mPlayer.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}