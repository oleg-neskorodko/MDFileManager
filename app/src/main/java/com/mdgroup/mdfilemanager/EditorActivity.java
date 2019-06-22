package com.mdgroup.mdfilemanager;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


public class EditorActivity extends AppCompatActivity {

    private Toolbar editorToolbar;
    private EditText editorEditText;
    private String uriString;
    private Uri uri;
    private File file;
    private String initialText;
    private String currentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Log.d(MainActivity.TAG, "EditorActivity onCreate");

        uriString = getIntent().getStringExtra("uri");
        Log.d(MainActivity.TAG, "EditorActivity onCreate" + uriString);
        uri = Uri.parse(uriString);
        String type = getIntent().getStringExtra("type");

        editorToolbar = findViewById(R.id.editorToolbar);
        editorEditText = findViewById(R.id.editorEditText);

        setSupportActionBar(editorToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        file = new File(uriString);
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
        initialText = text.toString();
        editorEditText.setText(initialText);
    }

    private String getSelectedText() {
        int startSelection = editorEditText.getSelectionStart();
        int endSelection = editorEditText.getSelectionEnd();
        return editorEditText.getText().toString().substring(startSelection, endSelection);
    }

    private void saveDocument() {
        String text = editorEditText.getText().toString();
        try {
            //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput(uriString, this.MODE_PRIVATE));
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file), "Cp1251");
            outputStreamWriter.write(text);
            outputStreamWriter.close();
        }

        catch (IOException e) {
            Log.d(MainActivity.TAG, "File write failed: " + e.toString());
        }
        Toast.makeText(this, "saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.editor_save:
                saveDocument();
                break;
            case R.id.editor_paste:
                break;
            case R.id.editor_copy:
                break;
            case R.id.editor_cut:
                break;
            case R.id.editor_find:
                break;
            case R.id.editor_cursor_left:
                break;
            case R.id.editor_cursor_right:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
