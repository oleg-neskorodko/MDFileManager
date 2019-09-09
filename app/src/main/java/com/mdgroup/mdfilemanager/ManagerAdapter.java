package com.mdgroup.mdfilemanager;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class ManagerAdapter extends RecyclerView.Adapter<ManagerAdapter.ViewHolder> {

    private View.OnClickListener clickListener;
    private View.OnLongClickListener longClickListener;
    private EventListener eventListener;
    private CheckBoxListener checkBoxListener;
    private ArrayList<File> objects;
    private ArrayList<Boolean> checkBoxState;
    private Context context;
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yy");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("HH:mm");
    private DecimalFormat decimalFormat1 = new DecimalFormat("##.##");
    private DecimalFormat decimalFormat2 = new DecimalFormat("##");

    public ManagerAdapter(Context context, ArrayList<File> objects, ArrayList<Boolean> checkBoxState) {
        this.context = context;
        this.objects = objects;
        this.checkBoxState = checkBoxState;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setClickListener(View.OnClickListener callback) {
        clickListener = callback;
    }

    public void setLongClickListener(View.OnLongClickListener callback) {
        longClickListener = callback;
    }

    public void setCheckBoxListener(CheckBoxListener listener) {
        checkBoxListener = listener;
    }

/*    public void setCheckBoxListener(CompoundButton.OnCheckedChangeListener callback) {
        checkBoxListener = callback;
    }*/

    @Override
    public ManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Log.d(MainActivity.TAG, "onCreateViewHolder post size = " + getItemCount());

/*
        checkBoxState.clear();
        for (int i = 0; i < objects.size(); i++) {
            checkBoxState.add(false);
        }
*/

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.manager_item,
                parent, false);

        ManagerAdapter.ViewHolder holder = new ManagerAdapter.ViewHolder(v);


/*        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onClick(view);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                longClickListener.onLongClick(v);
                return false;
            }
        });*/

        return holder;
    }

    @Override
    public void onBindViewHolder(final ManagerAdapter.ViewHolder holder, final int position) {
        //Log.d(MainActivity.TAG, "onBindViewHolder post size = " + checkBoxState.get(position));

        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventListener.clickEvent(position);
            }
        });

        holder.itemView.setOnLongClickListener(null);
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                eventListener.longClickEvent(position);
                return false;
            }
        });

        holder.itemCheckBox.setOnCheckedChangeListener(null);
        holder.itemCheckBox.setChecked(checkBoxState.get(position));
        if (checkBoxState.get(position)) {
            holder.itemCheckBox.setButtonTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.colorGray1)));
        } else {
            holder.itemCheckBox.setButtonTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.colorGray3)));
        }

        holder.itemCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkBoxListener.onCheckedChanged(position, isChecked);
                if (isChecked) {
                    holder.itemCheckBox.setButtonTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.colorGray1)));
                    checkBoxState.set(position, true);
                } else {
                    holder.itemCheckBox.setButtonTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.colorGray3)));
                    checkBoxState.set(position, false);
                }
            }
        });

        holder.nameTextView.setText(objects.get(position).getName());
        long date = objects.get(position).lastModified();
        holder.dateTextView.setText(sdf2.format(date));
        holder.timeTextView.setText(sdf3.format(date));

        double size = (double) getFileSize(objects.get(position));
        //Log.d(MainActivity.TAG, "size = " + size);
        holder.discSpaceTextView.setText(setSizeString(size));
        holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(selectPicture(position)));
    }

    private String setSizeString(double size) {
        String output;
        if (size >= 0) {
            String measure = context.getString(R.string.bytes);
            if (size >= 1024) {
                size /= 1024;
                measure = context.getString(R.string.kilobytes);
                if (size >= 1024) {
                    size /= 1024;
                    measure = context.getString(R.string.megabytes);
                    if (size >= 1024) {
                        size /= 1024;
                        measure = context.getString(R.string.gigabytes);
                    }
                }
            }
            if (size < 10) {
                output = decimalFormat1.format(size) + " " + measure;
            } else {
                output = decimalFormat2.format(size) + " " + measure;
            }
        } else output = "";
        return output;
    }

    private int selectPicture(int position) {
        int[] pictureList = new int[] {
                R.drawable.folder,
                R.drawable.image,
                R.drawable.video_player,
                R.drawable.music,
                R.drawable.text,
                R.drawable.file
        };
        if (objects.get(position).isDirectory()) {
            return pictureList[0];
        } else {
            String mimeType = checkFileType(objects.get(position).getAbsolutePath());
            if (mimeType.startsWith("image")) {
                return pictureList[1];
            } else if (mimeType.startsWith("video")) {
                return pictureList[2];
            } else if (mimeType.startsWith("audio")) {
                return pictureList[3];
            } else if (mimeType.startsWith("text")) {
                return pictureList[4];
            } else {
                return pictureList[5];
            }
        }
    }

    private static long getFileSize(File file) {
        long size;
        if (file.isDirectory()) {
            size = -1;
        } else {
            size = file.length();
        }
        return size;
    }

    private String checkFileType(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        if (mimeType != null) {
            return mimeType;
        } else {
            return "unknown";
        }
    }

    @Override
    public int getItemCount() {
        if (objects == null)
            return 0;
        return objects.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView dateTextView;
        TextView timeTextView;
        TextView discSpaceTextView;
        ImageView pictureImageView;
        CheckBox itemCheckBox;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            discSpaceTextView = itemView.findViewById(R.id.discSpaceTextView);
            pictureImageView = itemView.findViewById(R.id.pictureImageView);
            itemCheckBox = itemView.findViewById(R.id.itemCheckBox);
        }
    }
}

