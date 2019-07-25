package com.mdgroup.mdfilemanager;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ManagerFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private FragmentInteractionListener listener;
    public static final String KEY_CURRENT_DIRECTORY = "CURRENT_DIRECTORY";
    private String mInitialDirectory;
    private TextView directoryTextView;
    private ManagerAdapter mListDirectoriesAdapter;
    private File mSelectedDir;
    private ArrayList<File> mFilesInDir;
    private ArrayList<File> filteredList;
    private ArrayList<File> displayedList;
    private ArrayList<Integer> checkedItems;
    private ArrayList<Integer> bufferedFilesIndexList;
    private ArrayList<String> bufferedFilesList;
    private ArrayList<Boolean> checkBoxState;
    private final int MY_PERMISSIONS_REQUEST = 1;
    private File initialDir;
    private boolean longPressed;
    private boolean cutFile;
    private String[] dialogItems;
    private String[] sortDialogItems;
    private String bufferedFilePath;
    private SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private int sortWay;
    private String searchString;

    public void setListener(FragmentInteractionListener listener) {
        this.listener = listener;
    }

    public String getCurrentDirectory() {
        return mSelectedDir.getAbsolutePath();
    }

    public void setCurrentDirectory(String dir) {
        mInitialDirectory = dir;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedDir != null) {
            outState.putString(KEY_CURRENT_DIRECTORY, mSelectedDir.getAbsolutePath());
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.manager_layout, null);
        Log.d(MainActivity.TAG, "ExplorerFragment onCreateView");

        listener.setCurrentDirectory();

        directoryTextView = (TextView) v.findViewById(R.id.directoryTextView);
        longPressed = false;
        cutFile = false;
        bufferedFilePath = "";

        initLists();

        mListDirectoriesAdapter = new ManagerAdapter(getActivity(), displayedList, checkBoxState);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        recyclerView.setAdapter(mListDirectoriesAdapter);

        setListeners();
        //requestPermissionWithRationale(v);

        if (!hasPermissions()) {
            requestPerms();
        } else {
            setInitialDir();
            onFileClick(initialDir);
        }
        return v;
    }

    private void initLists() {
        dialogItems = new String[]{getActivity().getResources().getString(R.string.copy),
                getActivity().getResources().getString(R.string.paste),
                getActivity().getResources().getString(R.string.cut),
                getActivity().getResources().getString(R.string.delete),
                getActivity().getResources().getString(R.string.rename)
        };

        sortDialogItems = new String[]{getActivity().getResources().getString(R.string.a_z),
                getActivity().getResources().getString(R.string.z_a),
                getActivity().getResources().getString(R.string.old_new),
                getActivity().getResources().getString(R.string.new_old),
                getActivity().getResources().getString(R.string.small_large),
                getActivity().getResources().getString(R.string.large_small)};

        mFilesInDir = new ArrayList<>();
        filteredList = new ArrayList<>();
        displayedList = new ArrayList<>();
        checkedItems = new ArrayList<>();
        bufferedFilesIndexList = new ArrayList<>();
        bufferedFilesList = new ArrayList<>();
        checkBoxState = new ArrayList<>();
    }

    private void setListeners() {
        mListDirectoriesAdapter.setClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!longPressed) {
                    int position = recyclerView.getChildAdapterPosition(v);
                    Log.d(MainActivity.TAG, "onClick position = " + position);
                    if (hasPermissions()) {
                        onFileClick(displayedList.get(position).getAbsoluteFile());
                    }
                } else longPressed = false;
            }
        });

        mListDirectoriesAdapter.setLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = recyclerView.getChildAdapterPosition(v);
                //Log.d(MainActivity.TAG, "onLongClick position = " + position);
                if (hasPermissions()) {
                    makeOptionsDialog(position);
                }
                return false;
            }
        });

        mListDirectoriesAdapter.setCheckBoxListener(new CheckBoxListener() {
            @Override
            public void onCheckedChanged(int position, boolean isChecked) {
                //Log.d(MainActivity.TAG, "checkedChanged = " + position + isChecked + checkedItems.size());
                if (isChecked) {
                    checkedItems.add(position);
                } else {
                    for (int i = 0; i < checkedItems.size(); i++) {
                        if (checkedItems.get(i) == position) {
                            checkedItems.remove(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    public void filterList(String string) {
        //Log.d(MainActivity.TAG, "ExplorerFragment filterList = " + string);
        filteredList.clear();
        searchString = string;
        if (string.equals("")) {
            displayedList.clear();
            displayedList.addAll(mFilesInDir);
            recyclerView.getAdapter().notifyDataSetChanged();
        } else if (mFilesInDir.size() > 0) {
            for (int i = 0; i < mFilesInDir.size(); i++) {

                if (mFilesInDir.get(i).getName().toLowerCase().contains(string.toLowerCase())) {
                    filteredList.add(mFilesInDir.get(i));
                }
            }
            displayedList.clear();
            displayedList.addAll(filteredList);
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    public void unfilterList() {
        displayedList.clear();
        displayedList.addAll(mFilesInDir);
        searchString = "";
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void onSortListPressed() {
        makeSortDialog();
    }

    private void makeSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);
        builder.setTitle(R.string.sort_list);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final String[] leftSymbols = {
                "A",
                "Z",
                "\uD83D\uDD52",
                "\uD83D\uDD57",
                "\uD83D\uDDCB",
                "\uD83D\uDDCB"
        };

        final String[] rightSymbols = {
                "Z",
                "A",
                "",
                "",
                "\uD83D\uDDCB",
                "\uD83D\uDDCB"
        };

        final int[] leftSymbolSizes = {24, 24, 32, 32, 20, 28};
        final int[] rightSymbolSizes = {24, 24, 8, 8, 28, 20};

        ListAdapter adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, sortDialogItems) {

            DialogViewHolder holder;
            public View getView(int position, View convertView, ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.list_item, null);

                    holder = new DialogViewHolder();
                    holder.iconImageView = convertView.findViewById(R.id.iconImageView);
                    holder.titleTextView = convertView.findViewById(R.id.titleTextView);
                    holder.leftSymbolTextView = convertView.findViewById(R.id.leftSymbolTextView);
                    holder.rightSymbolTextView = convertView.findViewById(R.id.rightSymbolTextView);
                    convertView.setTag(holder);
                } else {
                    holder = (DialogViewHolder) convertView.getTag();
                }

                holder.titleTextView.setText(sortDialogItems[position]);
                holder.iconImageView.setVisibility(View.GONE);

                Typeface customFont = ResourcesCompat.getFont(getActivity(), R.font.symbola);
                holder.leftSymbolTextView.setTypeface(customFont);
                holder.rightSymbolTextView.setTypeface(customFont);
                holder.leftSymbolTextView.setText(leftSymbols[position]);
                holder.rightSymbolTextView.setText(rightSymbols[position]);
                holder.leftSymbolTextView.setTextSize(leftSymbolSizes[position]);
                holder.rightSymbolTextView.setTextSize(rightSymbolSizes[position]);

                return convertView;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sortFiles(which);
            }
        });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getActivity().getResources().getColor(R.color.colorIcon2));
            }
        });
        alert.show();
    }

    private void sortFiles(int way) {
        ArrayList<File> dirList = new ArrayList<>();
        ArrayList<File> fileList = new ArrayList<>();
        Comparator<File> comparator;
        Comparator<File> letterComparator = null;

        for (int i = 0; i < mFilesInDir.size(); i++) {
            if (mFilesInDir.get(i).isDirectory()) {
                dirList.add(mFilesInDir.get(i));
            } else {
                fileList.add(mFilesInDir.get(i));
            }
        }

        if (way == 2 || way == 3) {
            comparator = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return Long.compare(o1.lastModified(), o2.lastModified());
                }
            };
        } else if (way == 0 || way == 1) {
            comparator = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            };
        } else {
            comparator = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return Long.compare(o1.length(), o2.length());
                }
            };
            letterComparator = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            };
        }

        Collections.sort(dirList, letterComparator);
        Collections.sort(fileList, comparator);
        if (way == 1 || way == 3 || way == 5) {
            Collections.reverse(fileList);
        }

        mFilesInDir.clear();
        mFilesInDir.addAll(dirList);
        mFilesInDir.addAll(fileList);
        checkedItems.clear();
        refreshCheckBoxState(mFilesInDir.size());
        displayedList.clear();
        displayedList.addAll(mFilesInDir);
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private String checkFileType(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        if (mimeType != null) {
            return mimeType;
        } else {
            return "unknown";
        }
    }

    public void createNewFolder() {
        String initialName = getActivity().getResources().getString(R.string.new_folder_name);
        String suggestedName = initialName;
        int number = 1;
        for (int j = 0; j < 10000; j++) {
            String startLoopName = suggestedName;
            for (int i = 0; i < mFilesInDir.size(); i++) {
                if (suggestedName.equals(mFilesInDir.get(i).getName())) {
                    number++;
                    suggestedName = initialName.concat(String.valueOf(number));
                    break;
                }
            }
            if (suggestedName.equals(startLoopName)) {
                break;
            }
        }
        File newFolder = new File(mSelectedDir, suggestedName);
        if (newFolder.mkdir()) {
            mFilesInDir.add(newFolder);
            displayedList.clear();
            displayedList.addAll(mFilesInDir);
            checkBoxState.add(false);
            renameFile(mFilesInDir.size() - 1, true, null);
        } else {
            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_folder_created), Toast.LENGTH_SHORT).show();
        }
    }

    public void createNewDocument() {
        Log.d(MainActivity.TAG, "createNewFile");
        String fileExtension = ".txt";
        String initialName = getActivity().getResources().getString(R.string.new_file_name);
        String suggestedName = initialName;
        int number = 1;
        for (int j = 0; j < 10000; j++) {
            String startLoopName = suggestedName;
            for (int i = 0; i < mFilesInDir.size(); i++) {
                if ((suggestedName + fileExtension).equals(mFilesInDir.get(i).getName())) {
                    number++;
                    suggestedName = initialName.concat(String.valueOf(number));
                    break;
                }
            }
            if (suggestedName.equals(startLoopName)) {
                break;
            }
        }
        File newFile = new File(mSelectedDir, suggestedName + fileExtension);
        try {
            if (newFile.createNewFile()) {
                mFilesInDir.add(newFile);
                displayedList.clear();
                displayedList.addAll(mFilesInDir);
                checkBoxState.add(false);
                renameFile(mFilesInDir.size() - 1, true, null);
            } else {
                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_file_created), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeOptionsDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);
        builder.setTitle(R.string.choose);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final int[] icons = {
                R.drawable.copy,
                R.drawable.paste,
                R.drawable.cut,
                R.drawable.delete,
                R.drawable.rename,
        };

        ListAdapter adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, dialogItems) {

            DialogViewHolder holder;
            public View getView(int position, View convertView, ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.list_item, null);
                    Log.d(MainActivity.TAG, "convertView null");

                    holder = new DialogViewHolder();
                    holder.iconImageView = convertView.findViewById(R.id.iconImageView);
                    holder.titleTextView = convertView.findViewById(R.id.titleTextView);
                    holder.leftSymbolTextView = convertView.findViewById(R.id.leftSymbolTextView);
                    holder.rightSymbolTextView = convertView.findViewById(R.id.rightSymbolTextView);
                    convertView.setTag(holder);
                } else {
                    Log.d(MainActivity.TAG, "convertView defined");
                    holder = (DialogViewHolder) convertView.getTag();
                }

                holder.titleTextView.setText(dialogItems[position]);
                holder.leftSymbolTextView.setVisibility(View.GONE);
                holder.rightSymbolTextView.setVisibility(View.GONE);
                holder.iconImageView.setImageResource(icons[position]);
                Log.d(MainActivity.TAG, "adapter ready");
                return convertView;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean applyToGroup = false;
                if (checkedItems.size() > 0) {
                    for (int i = 0; i < checkedItems.size(); i++) {
                        if (checkedItems.get(i) == position) {
                            applyToGroup = true;
                            break;
                        }
                    }
                }
                optionSwitcher(position, applyToGroup, which);
            }
        });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getActivity().getResources().getColor(R.color.colorIcon2));
            }
        });
        alert.show();
    }

    private void optionSwitcher(int position, boolean applyToGroup, int option) {
        switch (option) {
            case 0:
                Log.d(MainActivity.TAG, "copy");
                bufferedFilePath = "";
                bufferedFilesList.clear();
                if (applyToGroup) {
                    getPaths(checkedItems);
                } else {
                    getPath(position);
                }
                listener.setPasteIconState(true);
                cutFile = false;
                break;
            case 1:
                Log.d(MainActivity.TAG, "paste, cutFile = " + cutFile);
                if (bufferedFilePath.equals("") && bufferedFilesList.size() == 0) {
                    Toast.makeText(getActivity(), getString(R.string.no_file_to_paste), Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!displayedList.get(position).isDirectory()) {
                    Toast.makeText(getActivity(), getString(R.string.select_dst_folder), Toast.LENGTH_SHORT).show();
                    break;
                }
                Log.d(MainActivity.TAG, "paste");
                listener.setPasteIconState(false);
                if (!bufferedFilePath.equals("")) {
                    if (cutFile && pasteFile(bufferedFilePath, displayedList.get(position).getAbsolutePath())) {
                        deleteFile(bufferedFilePath, false);
                        bufferedFilePath = "";
                    }
                } else if (bufferedFilesList != null && bufferedFilesList.size() > 0) {
                    ArrayList<Integer> pastedFiles = new ArrayList<>();
                    pastedFiles = pasteFiles(bufferedFilesList, displayedList.get(position).getAbsolutePath());
                    if (cutFile) {
                        ArrayList<String> filesToDelete = new ArrayList<>();
                        for (int i : pastedFiles) {
                            filesToDelete.add(bufferedFilesList.get(i));
                        }
                        deleteFiles(filesToDelete);
                        bufferedFilesList.clear();
                    }
                }
                break;
            case 2:
                Log.d(MainActivity.TAG, "cut");
                bufferedFilePath = "";
                bufferedFilesList.clear();
                if (applyToGroup) {
                    getPaths(checkedItems);
                } else {
                    getPath(position);
                }
                listener.setPasteIconState(true);
                cutFile = true;
                break;
            case 3:
                Log.d(MainActivity.TAG, "delete");
                if (applyToGroup) {
                    Log.d(MainActivity.TAG, "applyToGroup");
                    deleteFiles(checkedItems, true);
                } else {
                    deleteFile(position, true);
                }
                break;
            case 4:
                Log.d(MainActivity.TAG, "rename");
                if (applyToGroup) {
                    renameFile(position, false, checkedItems);
                } else {
                    renameFile(position, false, null);
                }
                break;
        }
    }

    public void pasteFileIntoCurrentDirectory() {
        if (!bufferedFilePath.equals("")) {
            if (cutFile & pasteFile(bufferedFilePath, mSelectedDir.getAbsolutePath())) {
                deleteFile(bufferedFilePath, false);
                bufferedFilePath = "";
            }
        } else if (bufferedFilesList != null && bufferedFilesList.size() > 0) {
            ArrayList<Integer> pastedFiles = new ArrayList<>();
            pastedFiles = pasteFiles(bufferedFilesList, mSelectedDir.getAbsolutePath());
            if (cutFile) {
                ArrayList<String> filesToDelete = new ArrayList<>();
                for (int i : pastedFiles) {
                    filesToDelete.add(bufferedFilesList.get(i));
                }
                deleteFiles(filesToDelete);
                bufferedFilesList.clear();
            }
        }
        refreshLists(mSelectedDir);
    }

    private String silentRename(String dstDir, String inputName, boolean isDirectory) {
        Log.d(MainActivity.TAG, "silentRename");
        File dstDirectory = new File(dstDir);
        File[] dstDirContents = dstDirectory.listFiles();

        String outputName = inputName;
        int number = 1;
        if (isDirectory) {
            Log.d(MainActivity.TAG, "silentRename, isDirectory");
            for (int j = 0; j < 10000; j++) {
                String startLoopName = outputName;
                for (int i = 0; i < dstDirContents.length; i++) {
                    if (outputName.equals(dstDirContents[i].getName())) {
                        number++;
                        outputName = inputName.concat("(" + number + ")");
                        break;
                    }
                }
                if (outputName.equals(startLoopName)) {
                    break;
                }
            }
        } else {
            int lastDotIndex = inputName.lastIndexOf('.');
            String name = "";
            String extension = "";
            name = inputName.substring(0, lastDotIndex);
            extension = inputName.substring(lastDotIndex);

            if (lastDotIndex > 0) {
                Log.d(MainActivity.TAG, "silentRename, lastDotIndex > 0");

                for (int j = 0; j < 10000; j++) {
                    String startLoopName = outputName;
                    for (int i = 0; i < dstDirContents.length; i++) {
                        if (outputName.equals(dstDirContents[i].getName())) {
                            number++;
                            outputName = name.concat("(" + number + ")" + extension);
                            break;
                        }
                    }
                    if (outputName.equals(startLoopName)) {
                        break;
                    }
                }

            } else {
                Log.d(MainActivity.TAG, "silentRename, lastDotIndex <= 0");
                for (int j = 0; j < 10000; j++) {
                    String startLoopName = outputName;
                    for (int i = 0; i < dstDirContents.length; i++) {
                        if (outputName.equals(dstDirContents[i].getName())) {
                            number++;
                            outputName = inputName.concat("(" + number + ")");
                            break;
                        }
                    }
                    if (outputName.equals(startLoopName)) {
                        break;
                    }
                }
            }
        }
        return outputName;
    }

    private void renameFile(final int position, final boolean isNewFile, final ArrayList<Integer> inputList) {
        Log.d(MainActivity.TAG, "renameFile");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);

        final ArrayList<Integer> renameList = new ArrayList<>();

        if (inputList != null && inputList.size() > 0) {
            renameList.addAll(bubbleSort(inputList));
        } else {
            renameList.add(position);
        }

        final EditText inputEditText = new EditText(getActivity());
        inputEditText.setSingleLine();
        inputEditText.setTypeface(Typeface.MONOSPACE);
        File file = displayedList.get(position);
        final String initialPath = file.getAbsolutePath();
        final String initialName = file.getName();
        inputEditText.setText(initialName);
        FrameLayout container = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 40;
        params.bottomMargin = 40;
        params.leftMargin = 20;
        params.rightMargin = 20;
        inputEditText.setLayoutParams(params);
        container.addView(inputEditText);
        builder.setView(container);

        if (file.isDirectory()) {
            inputEditText.setSelection(initialName.length());
        } else if (initialName.contains(".")) {
            inputEditText.setSelection(initialName.lastIndexOf("."));
        } else {
            inputEditText.setSelection(initialName.length());
        }

        builder.setTitle(getActivity().getResources().getString(R.string.insert_new_name))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
                        inputEditText.clearFocus();
                        if (isNewFile) {
                            deleteFile(position, false);
                        }
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
                        inputEditText.clearFocus();

                        int successfulRenames = 0;
                        String inputName = inputEditText.getText().toString();

                        for (int index : renameList) {

                            if (index != position || !mFilesInDir.get(position).getName().equals(inputName)) {

                                String suggestedName = createNewName(inputName, index);
                                File newFile = new File(mSelectedDir.getAbsolutePath(), suggestedName);
                                if (mFilesInDir.get(index).renameTo(newFile)) {
                                    mFilesInDir.set(index, newFile);
                                    successfulRenames++;
                                }

                                if (!isNewFile) {
                                    if (!bufferedFilePath.equals("")) {
                                        if (bufferedFilePath.equals(initialPath)) {
                                            bufferedFilePath = mFilesInDir.get(index).getAbsolutePath();
                                        }
                                    } else if (bufferedFilesList != null && bufferedFilesList.size() > 0) {
                                        for (int i = 0; i < bufferedFilesList.size(); i++) {
                                            if (bufferedFilesList.get(i).equals(initialPath)) {
                                                bufferedFilesList.set(i, mFilesInDir.get(index).getAbsolutePath());
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isNewFile) {
                            String message = "";
                            if (successfulRenames == renameList.size()) {
                                message = successfulRenames + "\u0020files have been successfully renamed";
                            } else {
                                message = (renameList.size() - successfulRenames) + "\u0020files have not been renamed";
                            }
                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                            if (successfulRenames > 0) {
                                refreshLists(mSelectedDir);
                            }
                        } else {
                            refreshLists(mSelectedDir);
                        }
                    }
                });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getActivity().getResources().getColor(R.color.colorIcon2));
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getActivity().getResources().getColor(R.color.colorIcon2));
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(inputEditText.getApplicationWindowToken(),
                        InputMethodManager.SHOW_FORCED, 0);
            }
        });
        alert.show();
    }

    private String createNewName(String inputName, int index) {
        String suggestedName = inputName;
        int number = 1;

        if (mFilesInDir.get(index).isDirectory()) {
            for (int j = 0; j < 10000; j++) {
                String startLoopName = suggestedName;
                for (int i = 0; i < mFilesInDir.size(); i++) {
                    if (suggestedName.equals(mFilesInDir.get(i).getName())) {
                        number++;
                        suggestedName = inputName.concat("(" + String.valueOf(number) + ")");
                        break;
                    }
                }
                if (suggestedName.equals(startLoopName)) {
                    break;
                }
            }
        } else {
            int lastDotIndex = inputName.lastIndexOf('.');
            String name = "";
            String extension = "";
            name = inputName.substring(0, lastDotIndex);
            extension = inputName.substring(lastDotIndex);

            if (lastDotIndex > 0) {

                for (int j = 0; j < 10000; j++) {
                    String startLoopName = suggestedName;
                    for (int i = 0; i < mFilesInDir.size(); i++) {
                        if (suggestedName.equals(mFilesInDir.get(i).getName())) {
                            number++;
                            suggestedName = name.concat("(" + number + ")" + extension);
                            break;
                        }
                    }
                    if (suggestedName.equals(startLoopName)) {
                        break;
                    }
                }

            } else {
                name = inputName;
                for (int j = 0; j < 10000; j++) {
                    String startLoopName = suggestedName;
                    for (int i = 0; i < mFilesInDir.size(); i++) {
                        if (suggestedName.equals(mFilesInDir.get(i).getName())) {
                            number++;
                            suggestedName = inputName.concat("(" + String.valueOf(number) + ")");
                            break;
                        }
                    }
                    if (suggestedName.equals(startLoopName)) {
                        break;
                    }
                }
            }
        }
        return suggestedName;
    }

    private void setInitialDir() {
        if (!TextUtils.isEmpty(mInitialDirectory) && isValidFile(new File(mInitialDirectory))) {
            initialDir = new File(mInitialDirectory);
        } else {
            initialDir = Environment.getExternalStorageDirectory();
        }
    }

    public void onBackPressed() {
        if (mSelectedDir.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            listener.onFinishApp();
        } else {
            final File parent;
            if (mSelectedDir != null && (parent = mSelectedDir.getParentFile()) != null) {
                onFileClick(parent);
            }
        }
    }

    private ArrayList<Integer> bubbleSort(ArrayList<Integer> listToSort) {
        for (int i = 0; i < listToSort.size(); i++) {
            int min = listToSort.get(i);
            int position = i;
            int temp = 0;
            for (int j = i; j < listToSort.size(); j++) {
                if (min > listToSort.get(j)) {
                    min = listToSort.get(j);
                    position = j;
                }
            }
            temp = listToSort.get(i);
            listToSort.set(i, min);
            listToSort.set(position, temp);
        }
        return listToSort;
    }

    private void getPath(int position) {
        bufferedFilePath = displayedList.get(position).getAbsolutePath();
        bufferedFilesIndexList.clear();
        bufferedFilesList.clear();
    }

    private void getPaths(ArrayList<Integer> filesToBuffer) {
        bufferedFilePath = "";
        bufferedFilesIndexList = bubbleSort(filesToBuffer);
        for (int i = 0; i < bufferedFilesIndexList.size(); i++) {
            bufferedFilesList.add(displayedList.get(bufferedFilesIndexList.get(i)).getAbsolutePath());
        }
    }

    public ArrayList<Integer> pasteFiles(ArrayList<String> srcFiles, String dstDir) {
        Log.d(MainActivity.TAG, "pasteFiles, bufferdFiles = " + bufferedFilesList.size());
        ArrayList<Integer> pastedFiles = new ArrayList<>();
        for (int i = 0; i < srcFiles.size(); i++) {
            if (pasteFile(srcFiles.get(i), dstDir)) {
                pastedFiles.add(i);
            }
        }
        return pastedFiles;
    }

    public boolean pasteFile(String srcDir, String dstDir) {
        Log.d(MainActivity.TAG, "pasteFile :" + srcDir + " / " + dstDir);
        File src = new File(srcDir);
        File dstDirectory = new File(dstDir);
        File[] dstDirFiles = dstDirectory.listFiles();
        String dstFileName = src.getName();
        for (int i = 0; i < dstDirFiles.length; i++) {
            if (dstDirFiles[i].getName().equals(src.getName())) {
/*                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.such_name_exists), Toast.LENGTH_SHORT).show();
                return false;*/
                dstFileName = silentRename(dstDir, dstFileName, src.isDirectory());
            }
        }
        File dst = new File(dstDir, dstFileName);
        try {
            copyItem(src, dst);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void copyItem(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists()) {
                dst.mkdir();
            }

            String[] children = src.list();
            for (int i = 0; i < src.listFiles().length; i++) {
                copyItem(new File(src, children[i]), new File(dst, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] filesArray = file.listFiles();
            for (File entry : filesArray) {
                recursiveDelete(entry);
            }
            if (file.delete()) {
                Log.d(MainActivity.TAG, "file Deleted :" + file.getAbsolutePath());
                //refreshLists(mSelectedDir);
            } else {
                Log.d(MainActivity.TAG, "file not Deleted :" + file.getAbsolutePath());
            }
        } else {
            if (file.delete()) {
                Log.d(MainActivity.TAG, "file Deleted :" + file.getAbsolutePath());
                //refreshLists(mSelectedDir);
            } else {
                Log.d(MainActivity.TAG, "file not Deleted :" + file.getAbsolutePath());
            }
        }
    }

    //overloaded method
    public void deleteFile(String path, boolean showDialog) {
        Log.d(MainActivity.TAG, "deleteFile path");
        File file = new File(path);
        if (showDialog) {
            ArrayList<File> filesToDelete = new ArrayList<>();
            filesToDelete.add(file);
            confirmDeleteDialog(filesToDelete);
        } else {
            if (file.exists()) {
                recursiveDelete(file);
                if (!bufferedFilePath.equals("")) {
                    if (bufferedFilePath.equals(path)) {
                        bufferedFilePath = "";
                    }
                } else if (bufferedFilesList != null && bufferedFilesList.size() > 0) {
                    for (int i = 0; i < bufferedFilesList.size(); i++) {
                        if (bufferedFilesList.get(i).equals(path)) {
                            bufferedFilesList.remove(i);
                        }
                    }
                }
                Log.d(MainActivity.TAG, "bufferd files = " + bufferedFilesList.size());
            }
        }
        refreshLists(mSelectedDir);
    }

    //overloaded method
    public void deleteFile(int position, boolean showDialog) {
        Log.d(MainActivity.TAG, "deleteFile position");
        File file = displayedList.get(position);
        if (showDialog) {
            ArrayList<File> filesToDelete = new ArrayList<>();
            filesToDelete.add(file);
            confirmDeleteDialog(filesToDelete);
        } else {
            if (file.exists()) {
                recursiveDelete(file);
                if (!bufferedFilePath.equals("")) {
                    if (bufferedFilePath.equals(displayedList.get(position).getAbsolutePath())) {
                        bufferedFilePath = "";
                    }
                } else if (bufferedFilesList != null && bufferedFilesList.size() > 0) {
                    for (int i = 0; i < bufferedFilesList.size(); i++) {
                        if (bufferedFilesList.get(i).equals(displayedList.get(position).getAbsolutePath())) {
                            bufferedFilesList.remove(i);
                        }
                    }
                }
                Log.d(MainActivity.TAG, "bufferd files = " + bufferedFilesList.size());
            }
        }
        refreshLists(mSelectedDir);
    }

    //overloaded method
    public void deleteFiles(ArrayList<Integer> inputList, boolean showDialog) {
        Log.d(MainActivity.TAG, "deleteFiles list int");
        if (showDialog) {
            ArrayList<File> filesToDelete = new ArrayList<>();
            for (int i = 0; i < inputList.size(); i++) {
                File file = mFilesInDir.get(inputList.get(i));
                filesToDelete.add(file);
            }
            confirmDeleteDialog(filesToDelete);
            checkedItems.clear();
        }
    }

    //overloaded method
    public void deleteFiles(ArrayList<String> filesToDelete) {
        Log.d(MainActivity.TAG, "deleteFiles list String");

        for (int i = 0; i < filesToDelete.size(); i++) {
            File file = new File(filesToDelete.get(i));
            if (file.exists()) {
                recursiveDelete(file);
                if (!bufferedFilePath.equals("")) {
                    if (bufferedFilePath.equals(file.getAbsolutePath())) {
                        bufferedFilePath = "";
                    }
                } else if (bufferedFilesList != null && bufferedFilesList.size() > 0) {
                    for (int j = 0; j < bufferedFilesList.size(); j++) {
                        if (bufferedFilesList.get(j).equals(file.getAbsolutePath())) {
                            bufferedFilesList.remove(j);
                        }
                    }

                }
                Log.d(MainActivity.TAG, "bufferd files = " + bufferedFilesList.size());
            }
        }
        checkedItems.clear();
        refreshLists(mSelectedDir);
    }

    private void confirmDeleteDialog(final ArrayList<File> filesToDelete) {
        String message = "";

        if (filesToDelete != null && filesToDelete.size() > 1) {
            int number = filesToDelete.size();
            message = getString(R.string.are_you_sure_multiple) + number + getString(R.string.are_you_sure_items);
        } else {
            message = getString(R.string.are_you_sure_single);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);
        builder.setTitle(message)
                .setCancelable(true)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (File file : filesToDelete) {
                            if (file.exists()) {
                                recursiveDelete(file);
                                if (!bufferedFilePath.equals("")) {
                                    if (bufferedFilePath.equals(file.getAbsolutePath())) {
                                        bufferedFilePath = "";
                                    }
                                } else if (bufferedFilesList != null && bufferedFilesList.size() > 0) {
                                    for (int i = 0; i < bufferedFilesList.size(); i++) {
                                        if (bufferedFilesList.get(i).equals(file.getAbsolutePath())) {
                                            bufferedFilesList.remove(i);
                                        }
                                    }
                                }
                            }
                        }
                        refreshLists(mSelectedDir);
                        dialog.dismiss();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getActivity().getResources().getColor(R.color.colorIcon2));
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getActivity().getResources().getColor(R.color.colorIcon2));
            }
        });
        alert.show();
    }

    private boolean isValidFile(final File file) {
        return (file != null && file.canWrite());
    }

    private void onFileClick(final File file) {
        Log.d(MainActivity.TAG, "onFileClick");
        if (file == null) {
            Log.d(MainActivity.TAG, "file is null");
        } else if (!file.isDirectory()) {
            String mimeType = checkFileType(file.getAbsolutePath());
            Log.d(MainActivity.TAG, "mimeType = " + mimeType);
            if (mimeType.startsWith("text") && !mimeType.equals("text/html") ||
                    file.getName().contains(".swift") || file.getName().contains(".kt") || file.getName().contains(".js")) {
                Intent intent = new Intent(getActivity(), EditorActivity.class);
                intent.putExtra("uri", file.getAbsolutePath());
                intent.putExtra("type", "text");
                startActivity(intent);
            } else {
                Log.d(MainActivity.TAG, "target");
                Intent target = new Intent(Intent.ACTION_VIEW);
                Uri targetUri = FileProvider.getUriForFile(getActivity(), getActivity()
                        .getApplicationContext().getPackageName() + ".fileprovider", file);
                Log.d(MainActivity.TAG, "data = " + targetUri.toString());
                target.setDataAndType(targetUri, mimeType);
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Log.d(MainActivity.TAG, "target ready = " + mimeType);

                PackageManager packageManager = getActivity().getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(target, 0);
                boolean isIntentSafe = activities.size() > 0;
                Log.d(MainActivity.TAG, "isIntentSafe = " + isIntentSafe);
                if (!isIntentSafe) {
                    target.setDataAndType(targetUri, "*/*");
                }
                startActivity(target);
            }
        } else {
            sortWay = 0;
            String path1 = file.getAbsolutePath();
            String path2 = path1.replace("/", " > ");
            directoryTextView.setText(path2);
            checkBoxState.clear();
            refreshLists(file);
            mSelectedDir = file;
        }
    }

    private void refreshCheckBoxState(int numberFiles) {
        if (checkBoxState.size() == 0) {
            for (int i = 0; i < numberFiles; i++) {
                checkBoxState.add(false);
            }
        }
    }

    private void refreshLists(File file) {
        Log.d(MainActivity.TAG, "refreshLists");
        searchString = "";
        final File[] contents = file.listFiles();
        checkBoxState.clear();
        refreshCheckBoxState(contents.length);
        if (contents != null) {
            int numDirectories = 0;
            for (final File f : contents) {
                numDirectories++;
            }
            mFilesInDir.clear();
            for (int i = 0, counter = 0; i < numDirectories; counter++) {
                mFilesInDir.add(contents[counter]);
                i++;
            }
            displayedList.clear();
            displayedList.addAll(mFilesInDir);
            sortFiles(sortWay);
        }
    }


    // RUNTIME PERMISSION METHODS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Log.d(MainActivity.TAG, "ExplorerFragment onRequestPermissionsResult");
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setInitialDir();
                onFileClick(initialDir);
            }
            //TODO
        }
    }

    private boolean hasPermissions() {
        //Log.d(MainActivity.TAG, "ExplorerFragment hasPermissions");
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else return false;
    }

    private void requestPerms() {
        //Log.d(MainActivity.TAG, "ExplorerFragment requestPerms");
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST);
    }

/*    public void requestPermissionWithRationale(View view) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            final String message = "Storage permission is needed to show files";
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestPerms();
                        }
                    })
                    .show();
        } else {
            requestPerms();
        }
    }*/

    public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        public SimpleDividerItemDecoration(Context context) {
            mDivider = context.getResources().getDrawable(R.drawable.line_divider);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}



