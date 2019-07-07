package com.mdgroup.mdfilemanager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class ManagerAdapter extends RecyclerView.Adapter<ManagerAdapter.ViewHolder> {

    private View.OnClickListener clickListener;
    private View.OnLongClickListener longClickListener;
    private ArrayList<File> objects;
    private Context context;
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yy");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("HH:mm");
    private DecimalFormat decimalFormat1 = new DecimalFormat("##.##");
    private DecimalFormat decimalFormat2 = new DecimalFormat("##");

    public ManagerAdapter(Context context, ArrayList<File> objects) {
        this.context = context;
        this.objects = objects;
    }

    public void setClickListener(View.OnClickListener callback) {
        clickListener = callback;
    }

    public void setLongClickListener(View.OnLongClickListener callback) {
        longClickListener = callback;
    }

    @Override
    public ManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Log.d(MainActivity.TAG, "onCreateViewHolder post size = " + getItemCount());

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.manager_item,
                parent, false);

        ManagerAdapter.ViewHolder holder = new ManagerAdapter.ViewHolder(v);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
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
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(ManagerAdapter.ViewHolder holder, int position) {
        //Log.d(MainActivity.TAG, "onBindViewHolder post size = " + getItemCount());

        holder.nameTextView.setText(objects.get(position).getName());
        long date = objects.get(position).lastModified();
        holder.dateTextView.setText(sdf2.format(date));
        holder.timeTextView.setText(sdf3.format(date));

        double size = (double) getFileSize(objects.get(position));
        //Log.d(MainActivity.TAG, "size = " + size);
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
                holder.discSpaceTextView.setText(decimalFormat1.format(size) + " " + measure);
            } else {
                holder.discSpaceTextView.setText(decimalFormat2.format(size) + " " + measure);
            }
        } else holder.discSpaceTextView.setText("");


        if (objects.get(position).isDirectory()) {
            holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.folder));
        } else {
            String mimeType = checkFileType(objects.get(position).getAbsolutePath());
            //Log.d(MainActivity.TAG, "mimeType = " + str);
            if (mimeType.startsWith("image")) {
                holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.image));
            } else if (mimeType.startsWith("video")) {
                holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.video_player));
            } else if (mimeType.startsWith("audio")) {
                holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.music));
            } else if (mimeType.startsWith("text")) {
                holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.text));
            } else {
                holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.file));
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

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            discSpaceTextView = itemView.findViewById(R.id.discSpaceTextView);
            pictureImageView = itemView.findViewById(R.id.pictureImageView);
        }
    }
}

