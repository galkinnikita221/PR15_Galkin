package com.nik.pr15_galkin;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.View;
import android.Manifest;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String fileName;
    private String userFileName;
    private Uri selectedDirUri;
    private static final int REQUEST_CODE_PICK_DIR = 1;
    private static final int REQUEST_CODE_PICK_AUDIO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/record.3gpp";
    }

    public void realeaseRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    public void recordStart(View v) {
        try {
            realeaseRecorder();
            File outFile = new File(fileName);
            if (outFile.exists()) {
                outFile.delete();
            }
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(fileName);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recordStop(View v) {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            showFileNameDialog();
        }
    }

    private void showFileNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сохранить файл");

        // Создаем EditText для ввода имени файла
        final EditText input = new EditText(this);
        input.setHint("Введите имя файла");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                userFileName = input.getText().toString();
                pickDirectory();
            }
        });

        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Ничего не делаем, просто закрываем диалог
            }
        });

        builder.show();
    }

    private void pickDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_DIR);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_DIR && resultCode == RESULT_OK) {
            if (data != null) {
                selectedDirUri = data.getData();
                saveRecordingToDirectory(userFileName);
            }
        } else if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri audioUri = data.getData();
                playSelectedAudio(audioUri);
            }
        }
    }

    private void saveRecordingToDirectory(String userFileName) {
        if (selectedDirUri != null) {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, selectedDirUri);
            if (pickedDir != null) {
                DocumentFile newFile = pickedDir.createFile("audio/3gpp", userFileName + ".3gpp");
                if (newFile != null) {
                    copyFile(new File(fileName), newFile);
                }
            }
        }
    }

    private void copyFile(File src, DocumentFile dst) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = getContentResolver().openOutputStream(dst.getUri())) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void playStart(View v) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO);
    }

    public void playStop(View v) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    private void playSelectedAudio(Uri audioUri) {
        try {
            releasePlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), audioUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}