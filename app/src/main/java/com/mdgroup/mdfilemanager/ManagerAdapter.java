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
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class ManagerAdapter extends RecyclerView.Adapter<ManagerAdapter.ViewHolder> {

    private View.OnClickListener clickListener;
    private View.OnLongClickListener longClickListener;
    private ArrayList<File> objects;
    private Context context;
    private SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yy HH:mm");
    private SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yy");
    private SimpleDateFormat sdf3 = new SimpleDateFormat("HH:mm");

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
            } else {
                holder.pictureImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.file));
            }
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
        ImageView pictureImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            pictureImageView = itemView.findViewById(R.id.pictureImageView);
        }
    }
}

