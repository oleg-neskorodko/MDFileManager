package com.mdgroup.mdfilemanager;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
    private final int MY_PERMISSIONS_REQUEST = 1;
    private File initialDir;
    private boolean longPressed;
    private boolean cutFile;
    private String dialogItems[];
    private String sortDialogItems[];
    private String bufferedFilePath;
    private SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private int sortWay;
    private String searchString;


    public void setListener(FragmentInteractionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedDir != null) {
            outState.putString(KEY_CURRENT_DIRECTORY, mSelectedDir.getAbsolutePath());
        }
    }

    public void filterList(String string) {
        Log.d(MainActivity.TAG, "ExplorerFragment filterList = " + string);
        filteredList.clear();
        searchString = string;
        if (string.equals("")) {
            displayedList.clear();
            displayedList.addAll(mFilesInDir);
            recyclerView.getAdapter().notifyDataSetChanged();
        } else if (mFilesInDir.size() > 0) {
            for (int i = 0; i < mFilesInDir.size(); i++) {

                if (mFilesInDir.get(i).getName().toLowerCase().contains(string.toLowerCase())) {
                    Log.d(MainActivity.TAG, "work");
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
        builder.setTitle(R.string.sort_list)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setItems(sortDialogItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                Log.d(MainActivity.TAG, "a_z");
                                sortFiles(0);
                                break;
                            case 1:
                                Log.d(MainActivity.TAG, "z_a");
                                sortFiles(1);
                                break;
                            case 2:
                                Log.d(MainActivity.TAG, "old_new");
                                sortFiles(2);
                                break;
                            case 3:
                                Log.d(MainActivity.TAG, "new_old");
                                sortFiles(3);
                                break;
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void sortFiles(int way) {

        ArrayList<File> dirList = new ArrayList<>();
        ArrayList<File> fileList = new ArrayList<>();
        Comparator<File> comparator;

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
        } else {
            comparator = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            };
        }

        Collections.sort(dirList, comparator);
        Collections.sort(fileList, comparator);
        if (way == 1 || way == 3) {
            Collections.reverse(dirList);
            Collections.reverse(fileList);
        }

        mFilesInDir.clear();
        mFilesInDir.addAll(dirList);
        mFilesInDir.addAll(fileList);
        displayedList.clear();
        displayedList.addAll(mFilesInDir);
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }
    }

    private String checkFileType(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        if (mimeType != null) {
            return mimeType;
        } else {
            return "unknown";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.manager_layout, null);
        Log.d(MainActivity.TAG, "ExplorerFragment onCreateView");

        directoryTextView = (TextView) v.findViewById(R.id.directoryTextView);
        longPressed = false;
        cutFile = false;
        dialogItems = new String[] {getActivity().getResources().getString(R.string.copy),
                getActivity().getResources().getString(R.string.paste),
                getActivity().getResources().getString(R.string.cut),
                getActivity().getResources().getString(R.string.delete),
                getActivity().getResources().getString(R.string.rename)
        };

        sortDialogItems = new String[] {getActivity().getResources().getString(R.string.a_z),
                getActivity().getResources().getString(R.string.z_a),
                getActivity().getResources().getString(R.string.old_new),
                getActivity().getResources().getString(R.string.new_old)};


        bufferedFilePath = "";

        mFilesInDir = new ArrayList<>();
        filteredList = new ArrayList<>();
        displayedList = new ArrayList<>();
        mListDirectoriesAdapter = new ManagerAdapter(getActivity(), displayedList);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        recyclerView.setAdapter(mListDirectoriesAdapter);

        mListDirectoriesAdapter.setClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!longPressed) {
                    int position = recyclerView.getChildAdapterPosition(v);
                    Log.d(MainActivity.TAG, "onClick position = " + position);
                    if (hasPermissions()) {
                        changeDirectory(displayedList.get(position).getAbsoluteFile());
                    }
                } else longPressed = false;
            }
        });

        mListDirectoriesAdapter.setLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //longPressed = true;
                int position = recyclerView.getChildAdapterPosition(v);
                //Log.d(MainActivity.TAG, "onLongClick position = " + position);
                if (hasPermissions()) {
                    makeDialog(position);
                }
                return false;
            }
        });

        //requestPermissionWithRationale(v);

        if (!hasPermissions()) {
            requestPerms();
        } else {
            setInitialDir();
            changeDirectory(initialDir);
        }
        return v;
    }

    private void makeDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);
        builder.setTitle(R.string.choose)
                .setCancelable(true)
                .setItems(dialogItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                Log.d(MainActivity.TAG, "copy");
                                getPath(position);
                                cutFile = false;
                                break;
                            case 1:
                                if (bufferedFilePath.equals("")) {
                                    Toast.makeText(getActivity(), "no file to paste", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                                if (!displayedList.get(position).isDirectory()) {
                                    Toast.makeText(getActivity(), "please select destination folder", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                                Log.d(MainActivity.TAG, "paste");
                                if (cutFile) {
                                    copyFileOrDirectory(bufferedFilePath, displayedList.get(position).getAbsolutePath());
                                    deleteFile(bufferedFilePath);
                                    bufferedFilePath = "";
                                } else {
                                    copyFileOrDirectory(bufferedFilePath, displayedList.get(position).getAbsolutePath());
                                    bufferedFilePath = "";
                                }
                                break;
                            case 2:
                                Log.d(MainActivity.TAG, "cut");
                                getPath(position);
                                cutFile = true;
                                break;
                            case 3:
                                Log.d(MainActivity.TAG, "delete");
                                deleteFile(position);
                                break;
                            case 4:
                                Log.d(MainActivity.TAG, "rename");
                                renameFile(position);
                                break;
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void renameFile(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);

        final EditText input = new EditText(getActivity());
        input.setText(displayedList.get(position).getName());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        builder.setTitle(R.string.choose)
                .setTitle("Rename file/folder")
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = input.getText().toString();
                        File newFile = new File(mSelectedDir.getAbsolutePath(), newName);
                        Log.d(MainActivity.TAG, "path = " + mSelectedDir.getAbsolutePath());
                        boolean renameSuccessful = displayedList.get(position).renameTo(newFile);
                        Toast.makeText(getActivity(), "successful rename = " + renameSuccessful, Toast.LENGTH_SHORT).show();

                        Log.d(MainActivity.TAG, "files = " + mFilesInDir.get(position).getName());
                        Log.d(MainActivity.TAG, "files = " + mFilesInDir.get(position).getAbsolutePath());
                        Log.d(MainActivity.TAG, "displayed = " + displayedList.get(position).getName());
                        Log.d(MainActivity.TAG, "displayed = " + displayedList.get(position).getAbsolutePath());

                        final File[] contents = mSelectedDir.listFiles();
                        if (contents != null) {
                            int numDirectories = contents.length;
                            mFilesInDir.clear();
                            for (int i = 0, counter = 0; i < numDirectories; counter++) {
                                mFilesInDir.add(contents[counter]);
                                i++;
                            }
                        }
                            displayedList.clear();
                            displayedList.addAll(mFilesInDir);
                            sortFiles(sortWay);
                    }
                });
        builder.setView(input);
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Log.d(MainActivity.TAG, "ExplorerFragment onRequestPermissionsResult");
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setInitialDir();
                changeDirectory(initialDir);
            }
            //TODO
        }
    }

    private boolean hasPermissions() {
        //Log.d(MainActivity.TAG, "ExplorerFragment hasPermissions");
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
            return true;
        } else return false;
    }

    private void requestPerms() {
        //Log.d(MainActivity.TAG, "ExplorerFragment requestPerms");
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.WRITE_EXTERNAL_STORAGE},
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

    private void setInitialDir() {
        //Log.d(MainActivity.TAG, "ExplorerFragment setInitialDir");
        if (!TextUtils.isEmpty(mInitialDirectory) && isValidFile(new File(mInitialDirectory))) {
            initialDir = new File(mInitialDirectory);
        } else {
            initialDir = Environment.getExternalStorageDirectory();
        }
    }

    public void onBackPressed() {
        if (mSelectedDir.getAbsolutePath().equals(initialDir.getAbsolutePath())) {
            listener.onFinishApp();
        } else {
            final File parent;
            if (mSelectedDir != null && (parent = mSelectedDir.getParentFile()) != null) {
                changeDirectory(parent);
            }
        }
    }

    private void getPath(int position) {
        bufferedFilePath = displayedList.get(position).getAbsolutePath();
    }

    public static void copyFileOrDirectory(String srcDir, String dstDir) {

        try {
            File src = new File(srcDir);
            File dst = new File(dstDir, src.getName());

            if (src.isDirectory()) {

                String files[] = src.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    String src1 = (new File(src, files[i]).getPath());
                    String dst1 = dst.getPath();
                    copyFileOrDirectory(src1, dst1);

                }
            } else {
                copyFile(src, dst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    //overloaded method
    public void deleteFile(String path) {
        Log.d(MainActivity.TAG, "deleteFile");
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("file Deleted :" + file.getAbsolutePath());
            } else {
                System.out.println("file not Deleted :" + file.getAbsolutePath());
            }
        }
    }

    //overloaded method
    public void deleteFile(int position) {
        Log.d(MainActivity.TAG, "deleteFile");
        File file = displayedList.get(position);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("file Deleted :" + file.getAbsolutePath());
                //TODO
                //mFilesInDir
                for (int i = 0; i < mFilesInDir.size(); i++) {
                    if (mFilesInDir.get(i).getName().equals(file.getName())) {
                        mFilesInDir.remove(i);
                    }
                }
                displayedList.remove(position);
            } else {
                System.out.println("file not Deleted :" + file.getAbsolutePath());
            }
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private static void debug(final String message, final Object... args) {
        Log.d(MainActivity.TAG, String.format(message, args));
    }

    private boolean isValidFile(final File file) {
        return (file != null && file.canWrite());
    }

    /**
     * Change the directory that is currently being displayed.
     *
     * @param dir The file the activity should switch to. This File must be
     *            non-null and a directory, otherwise the displayed directory
     *            will not be changed
     */
    private void changeDirectory(final File dir) {
        Log.d(MainActivity.TAG, "changeDirectory");
        if (dir == null) {
            debug("Could not change folder: dir was null");
        } else if (!dir.isDirectory()) {
            String mimeType = checkFileType(dir.getAbsolutePath());
/*            if (mimeType.startsWith("image")) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("uri", dir.getAbsolutePath());
                intent.putExtra("type", "image");
                startActivity(intent);
            } else if (mimeType.startsWith("video")) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("uri", dir.getAbsolutePath());
                intent.putExtra("type", "video");
                startActivity(intent);
            } else if (mimeType.startsWith("audio")) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("uri", dir.getAbsolutePath());
                intent.putExtra("type", "audio");
                startActivity(intent);
            }
            else */
            if (mimeType.startsWith("text")) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("uri", dir.getAbsolutePath());
                intent.putExtra("type", "text");
                startActivity(intent);
            } else {
                Log.d(MainActivity.TAG, "target");
                Intent target = new Intent(Intent.ACTION_VIEW);
                target.setDataAndType(Uri.fromFile(dir), mimeType);
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                Intent intent = Intent.createChooser(target, "Open File");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.d(MainActivity.TAG, "error createChooser");
                    // Instruct the user to install a PDF reader here, or something
                }
            }
        } else {
            sortWay = 0;
            searchString = "";
            final File[] contents = dir.listFiles();
            String path1 = dir.getAbsolutePath();
            String path2 = path1.replace("/", " > ");
            directoryTextView.setText(path2);
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
                mSelectedDir = dir;
                sortFiles(sortWay);
                //recyclerView.getAdapter().notifyDataSetChanged();
                //debug("Changed directory to %s", dir.getAbsolutePath());
            }
        }
    }

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

