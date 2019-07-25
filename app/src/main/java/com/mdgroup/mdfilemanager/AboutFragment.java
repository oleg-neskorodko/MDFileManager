package com.mdgroup.mdfilemanager;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

import androidx.fragment.app.Fragment;


public class AboutFragment extends Fragment {

    private String marketLink;
    private String webLink;
    private Button rateAboutButton;
    private Button shareAboutButton;
    private int rateButtonWidth;
    private int shareButtonWidth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.about_layout, null);

        marketLink = getActivity().getResources().getString(R.string.market_link);
        webLink = getActivity().getResources().getString(R.string.web_link);

        rateAboutButton = v.findViewById(R.id.rateAboutButton);
        shareAboutButton = v.findViewById(R.id.shareAboutButton);

        ViewTreeObserver observer = rateAboutButton.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                rateAboutButton.getViewTreeObserver().removeOnPreDrawListener(this);
                rateButtonWidth = rateAboutButton.getMeasuredWidth();
                return true;
            }
        });

        observer = shareAboutButton.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                shareAboutButton.getViewTreeObserver().removeOnPreDrawListener(this);
                shareButtonWidth = shareAboutButton.getMeasuredWidth();
                levelButtons();
                return true;
            }
        });

        rateAboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(marketLink + getActivity().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(webLink + getActivity().getPackageName())));
                }
            }
        });

        shareAboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String message = webLink + getActivity().getPackageName();
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_via)));
            }
        });

        return v;
    }

    private void levelButtons() {
        Log.d(MainActivity.TAG, "levelButtons");
        if (shareButtonWidth > rateButtonWidth) {
            rateAboutButton.setWidth(shareButtonWidth);
        } else {
            shareAboutButton.setWidth(rateButtonWidth);
        }
    }
}
