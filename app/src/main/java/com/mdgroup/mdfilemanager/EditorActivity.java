package com.mdgroup.mdfilemanager;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;


public class EditorActivity extends AppCompatActivity {

    private final String charsetName = "Cp1251";
    private Toolbar editorToolbar;
    private EditText editorEditText;
    private String uriString;
    private Uri uri;
    private File file;
    private String initialText;
    private String currentText;
    private String searchText;
    private ArrayList<Integer> indexes;
    private int arrayFocus;
    private TextView nameEditorTextView;
    private ImageView saveEditorImageView;
    private ImageView findEditorImageView;
    private SearchView editorSearchView;
    private TextView backEditorTextView;
    private TextView forwardEditorTextView;
    private TextView foundEditorTextView;
    private LinearLayout findEditorLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MainActivity.TAG, "EditorActivity onCreate");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_editor);

        uriString = getIntent().getStringExtra("uri");
        uri = Uri.parse(uriString);
        file = new File(uriString);
        indexes = new ArrayList<>();
        editorToolbar = findViewById(R.id.editorToolbar);
        editorEditText = findViewById(R.id.editorEditText);

        nameEditorTextView = editorToolbar.findViewById(R.id.nameEditorTextView);
        saveEditorImageView = editorToolbar.findViewById(R.id.saveEditorImageView);
        findEditorImageView = editorToolbar.findViewById(R.id.findEditorImageView);
        editorSearchView = editorToolbar.findViewById(R.id.editorSearchView);
        backEditorTextView = editorToolbar.findViewById(R.id.backEditorTextView);
        forwardEditorTextView = editorToolbar.findViewById(R.id.forwardEditorTextView);
        foundEditorTextView = editorToolbar.findViewById(R.id.foundEditorTextView);
        findEditorLayout = editorToolbar.findViewById(R.id.findEditorLayout);

        Typeface customFont = ResourcesCompat.getFont(this, R.font.symbola);
        backEditorTextView.setTypeface(customFont);
        backEditorTextView.setText("\uD83E\uDC60");
        forwardEditorTextView.setTypeface(customFont);
        forwardEditorTextView.setText("\uD83E\uDC62");

        findEditorLayout.setVisibility(View.GONE);

        nameEditorTextView.setText(file.getName());

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.saveEditorImageView:
                        saveDocument();
                        break;
                    case R.id.findEditorImageView:
                        showSearchToolbar();
                        break;
                    case R.id.backEditorTextView:
                        moveSelection(-1);
                        break;
                    case R.id.forwardEditorTextView:
                        moveSelection(1);
                        break;
                }
            }
        };

        saveEditorImageView.setOnClickListener(clickListener);
        findEditorImageView.setOnClickListener(clickListener);
        backEditorTextView.setOnClickListener(clickListener);
        forwardEditorTextView.setOnClickListener(clickListener);

        editorSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchText = newText.toLowerCase();
                findText(1);
                return false;
            }
        });

        editorSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                hideSearchToolbar();
                return false;
            }
        });

        setSupportActionBar(editorToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        StringBuilder text = new StringBuilder();
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName));
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

    private void moveSelection(int step) {
        int number = indexes.size();
        if (number > 1) {
            if ((step == 1 && arrayFocus < number) || (step == -1 && arrayFocus > 1)) {
                arrayFocus = arrayFocus + step;
                foundEditorTextView.setText(arrayFocus + "/" + number);
                Spannable spannedText = new SpannableString(currentText);
                for (int i = 0; i < indexes.size(); i++) {
                    if (i == arrayFocus - 1) {
                        spannedText.setSpan(new BackgroundColorSpan(Color.BLUE), indexes.get(i),
                                indexes.get(i) + searchText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        spannedText.setSpan(new BackgroundColorSpan(Color.WHITE), indexes.get(i),
                                indexes.get(i) + searchText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                editorEditText.setText(spannedText);
                editorEditText.setSelection((indexes.get(arrayFocus - 1)));
            }
        }
    }

    private void showSearchToolbar() {
        nameEditorTextView.setVisibility(View.GONE);
        saveEditorImageView.setVisibility(View.GONE);
        findEditorImageView.setVisibility(View.GONE);
        findEditorLayout.setVisibility(View.VISIBLE);
        editorSearchView.setIconified(false);
    }

    private void hideSearchToolbar() {
        nameEditorTextView.setVisibility(View.VISIBLE);
        saveEditorImageView.setVisibility(View.VISIBLE);
        findEditorImageView.setVisibility(View.VISIBLE);
        findEditorLayout.setVisibility(View.GONE);
    }

    private void findText(int arrFocus) {
        arrayFocus = arrFocus;
        currentText = editorEditText.getText().toString();
        if (!searchText.equals("")) {
            indexes.clear();
            int currentIndex = currentText.toLowerCase().indexOf(searchText);
            while (currentIndex >= 0) {
                indexes.add(currentIndex);
                currentIndex = currentText.indexOf(searchText, currentIndex + 1);
            }
            if (indexes.size() > 0) {
                int number = indexes.size();
                foundEditorTextView.setText(arrayFocus + "/" + number);

                Spannable spannedText = new SpannableString(currentText);
                for (int i = 0; i < indexes.size(); i++) {
                    if (i == arrayFocus - 1) {
                        spannedText.setSpan(new BackgroundColorSpan(Color.BLUE), indexes.get(i),
                                indexes.get(i) + searchText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        spannedText.setSpan(new BackgroundColorSpan(Color.WHITE), indexes.get(i),
                                indexes.get(i) + searchText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                editorEditText.setText(spannedText);
                editorEditText.setSelection((indexes.get(arrayFocus - 1)));
            } else {
                foundEditorTextView.setText("0/0");
                editorEditText.setText(currentText);
            }
        } else {
            foundEditorTextView.setText("0/0");
            editorEditText.setText(currentText);
        }
    }

    private void exitConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle(getResources().getString(R.string.saving_changes))
                .setCancelable(true)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveDocument();
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNeutralButton(R.string.return_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorIcon2));
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorIcon2));
                alert.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorIcon2));
            }
        });
        alert.show();
    }

    private void saveDocument() {
        String text = editorEditText.getText().toString();
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file), charsetName);
            outputStreamWriter.write(text);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.d(MainActivity.TAG, "File write failed: " + e.toString());
        }
        initialText = text;
        Toast.makeText(this, getResources().getString(R.string.saved), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (initialText.equals(editorEditText.getText().toString())) {
            super.onBackPressed();
        } else {
            exitConfirmDialog();
        }
    }
}
