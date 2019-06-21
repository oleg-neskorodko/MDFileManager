package com.mdgroup.mdfilemanager;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
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

        directoryTextView = (TextView) v.findViewById(R.id.directoryTextView);
        longPressed = false;
        cutFile = false;
        dialogItems = new String[]{getActivity().getResources().getString(R.string.copy),
                getActivity().getResources().getString(R.string.paste),
                getActivity().getResources().getString(R.string.cut),
                getActivity().getResources().getString(R.string.delete),
                getActivity().getResources().getString(R.string.rename)
        };

        sortDialogItems = new String[]{getActivity().getResources().getString(R.string.a_z),
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
        builder.setTitle(R.string.sort_list);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        ListAdapter adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, sortDialogItems) {

            ViewHolder holder;
            class ViewHolder {
                ImageView icon;
                TextView titleTextView;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.list_item, null);

                    holder = new ViewHolder();
                    //holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.titleTextView = (TextView) convertView.findViewById(R.id.titleTextView);
                    convertView.setTag(holder);
                } else {
                    // view already defined, retrieve view holder
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.titleTextView.setText(sortDialogItems[position]);
                //holder.icon.setVisibility(View.GONE);
                return convertView;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
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
        for (int j = 0; j < 1000; j++) {
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
            renameFile(mFilesInDir.size() - 1);
        } else {
            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_folder_created), Toast.LENGTH_SHORT).show();
        }
    }

    private void makeDialog(final int position) {
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

            ViewHolder holder;
            class ViewHolder {
                //ImageView icon;
                TextView titleTextView;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.list_item, null);
                    Log.d(MainActivity.TAG, "convertView null");

                    holder = new ViewHolder();
                    //holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.titleTextView = (TextView) convertView.findViewById(R.id.titleTextView);
                    convertView.setTag(holder);
                } else {
                    Log.d(MainActivity.TAG, "convertView defined");
                    // view already defined, retrieve view holder
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.titleTextView.setText(dialogItems[position]);
                //holder.icon.setImageResource(icons[position]);
                Log.d(MainActivity.TAG, "adapter ready");
                return convertView;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Log.d(MainActivity.TAG, "copy");
                        getPath(position);
                        listener.setPasteIconState(true);
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
                        listener.setPasteIconState(false);
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
                        listener.setPasteIconState(true);
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

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getActivity().getResources().getColor(R.color.colorIcon2));
            }
        });
        alert.show();
    }

    private void renameFile(final int position) {
        Log.d(MainActivity.TAG, "new dialog = " + mFilesInDir.size());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);

        final EditText input = new EditText(getActivity());
        input.setText(displayedList.get(position).getName());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        builder.setTitle(getActivity().getResources().getString(R.string.insert_new_name))
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
                        if (renameSuccessful) {
                            refreshLists(mSelectedDir);
                        }
                    }
                });
        builder.setView(input);
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

    private void setInitialDir() {
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

    public void pasteFileIntoCurrentDirectory() {
        copyFileOrDirectory(bufferedFilePath, mSelectedDir.getAbsolutePath());
        refreshLists(mSelectedDir);
    }

    public void copyFileOrDirectory(String srcDir, String dstDir) {
        Log.d(MainActivity.TAG, "copyFileOrDirectory ");

        try {
            File src = new File(srcDir);

/*            File dstDirectory = new File(dstDir);
            File[] dstDirFiles = dstDirectory.listFiles();
            Log.d(MainActivity.TAG, "here " + srcDir + " / " + dstDir);

            String initialName = src.getName();
            String suggestedName = initialName;
            int number = 0;
            for (int j = 0; j < 1000; j++) {
                String startLoopName = suggestedName;
                for (int i = 0; i < dstDirFiles.length; i++) {
                    if (suggestedName.equals(dstDirFiles[i].getName())) {
                        number++;
                        suggestedName = initialName + " - copy" + number;
                        break;
                    }
                }
                if (suggestedName.equals(startLoopName)) {
                    break;
                }
            }
            Log.d(MainActivity.TAG, "here2 " + suggestedName);
            File dst = new File(dstDir, suggestedName);*/

            File dst = new File(dstDir, src.getName());

            if (src.isDirectory()) {

                String files[] = src.list();
                if (files.length == 0) {
                    copyFile(src, dst);
                } else {
                    int filesLength = files.length;
                    for (int i = 0; i < filesLength; i++) {
                        String src1 = (new File(src, files[i]).getPath());
                        String dst1 = dst.getPath();
                        copyFileOrDirectory(src1, dst1);
                    }
                }
            } else {
                Log.d(MainActivity.TAG, "copyFile launch ");
                copyFile(src, dst);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(MainActivity.TAG, "Exception " + e.toString());
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        Log.d(MainActivity.TAG, "copyFile " + sourceFile.getName() + " " + destFile.getName());
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }
/*        if (sourceFile.isDirectory()) {
            destFile.mkdir();
        }*/

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

    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] filesArray = file.listFiles();
            for (File entry : filesArray) {
                recursiveDelete(entry);
            }
            if (file.delete()) {
                Log.d(MainActivity.TAG, "file Deleted :" + file.getAbsolutePath());
            } else {
                Log.d(MainActivity.TAG, "file not Deleted :" + file.getAbsolutePath());
            }
        } else {
            if (file.delete()) {
                Log.d(MainActivity.TAG, "file Deleted :" + file.getAbsolutePath());
            } else {
                Log.d(MainActivity.TAG, "file not Deleted :" + file.getAbsolutePath());
            }
        }
    }

    //overloaded method
    public void deleteFile(String path) {
        Log.d(MainActivity.TAG, "deleteFile");
        File file = new File(path);
        if (file.exists()) {
            recursiveDelete(file);
            refreshLists(mSelectedDir);
        }
    }

    //overloaded method
    public void deleteFile(int position) {
        Log.d(MainActivity.TAG, "deleteFile");
        File file = displayedList.get(position);
        if (file.exists()) {
            recursiveDelete(file);
            refreshLists(mSelectedDir);
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private static void debug(final String message, final Object... args) {
        Log.d(MainActivity.TAG, String.format(message, args));
    }

    private boolean isValidFile(final File file) {
        return (file != null && file.canWrite());
    }

    private void changeDirectory(final File dir) {
        Log.d(MainActivity.TAG, "changeDirectory");
        if (dir == null) {
            debug("Could not change folder: dir was null");
        } else if (!dir.isDirectory()) {
            String mimeType = checkFileType(dir.getAbsolutePath());
            Log.d(MainActivity.TAG, "mimeType = " + mimeType);
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
            if (mimeType.startsWith("text") && !mimeType.equals("text/html")) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("uri", dir.getAbsolutePath());
                intent.putExtra("type", "text");
                startActivity(intent);
            } else {
                Log.d(MainActivity.TAG, "target");
                Intent target = new Intent(Intent.ACTION_VIEW);
                Uri targetUri = FileProvider.getUriForFile(getActivity(), getActivity()
                        .getApplicationContext().getPackageName() + ".fileprovider", dir);
                Log.d(MainActivity.TAG, "data = " + targetUri.toString());
                target.setDataAndType(targetUri, mimeType);
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Log.d(MainActivity.TAG, "target ready = " + mimeType);

                PackageManager packageManager = getActivity().getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(target, 0);
                boolean isIntentSafe = activities.size() > 0;
                Log.d(MainActivity.TAG, "isIntentSafe = " + isIntentSafe);

                if (isIntentSafe) {
                    Log.d(MainActivity.TAG, "isIntentSafe true");
                    startActivity(target);
                } else {
                    Log.d(MainActivity.TAG, "isIntentSafe false");
                    Toast.makeText(getActivity(), "no such app", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            sortWay = 0;
            String path1 = dir.getAbsolutePath();
            String path2 = path1.replace("/", " > ");
            directoryTextView.setText(path2);
            refreshLists(dir);
            mSelectedDir = dir;
        }
    }

    private void refreshLists(File dir) {
        searchString = "";
        final File[] contents = dir.listFiles();
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
                changeDirectory(initialDir);
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



