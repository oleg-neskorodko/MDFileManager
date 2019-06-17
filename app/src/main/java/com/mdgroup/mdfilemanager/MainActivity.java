package com.mdgroup.mdfilemanager;

import android.content.ClipData;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements FragmentInteractionListener{

    public static String TAG = "tag";
    private ManagerFragment managerFragment;
    private Toolbar toolbarTop;
    private ImageView iconToolbarImageView;
    private TextView toolbarTitleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate");
        setContentView(R.layout.activity_main);

        toolbarTop = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbarTop);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        iconToolbarImageView = toolbarTop.findViewById(R.id.iconToolbarImageView);
        toolbarTitleTextView = toolbarTop.findViewById(R.id.toolbarTitleTextView);

        managerFragment = new ManagerFragment();
        managerFragment.setListener(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, managerFragment, "manager_fragment").addToBackStack("main_stack").commit();
        } else {
            ManagerFragment fragment = (ManagerFragment) getSupportFragmentManager().findFragmentByTag("manager_fragment");
            if (fragment != null) {
                fragment.setListener(this);
            }
        }
        Log.d(TAG, "MainActivity onCreate end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu1, menu);
        createSearch(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
/*            case R.id.menu_remote:
                Toast.makeText(MainActivity.this, getResources().getString(R.string.remote_access), Toast.LENGTH_SHORT).show();
                break;*/
            case R.id.menu_sort:
                Toast.makeText(MainActivity.this, getString(R.string.sort), Toast.LENGTH_SHORT).show();
                if (managerFragment != null) {
                    managerFragment.onSortListPressed();
                }
                break;
            case R.id.menu_search:
                Toast.makeText(MainActivity.this, getString(R.string.search), Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createSearch(Menu menu) {
        //final MenuItem menuRemote = menu.findItem(R.id.menu_remote);
        final MenuItem menuSort = menu.findItem(R.id.menu_sort);
        final MenuItem menuSearch = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) menuSearch.getActionView();
        int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView v = (ImageView) searchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.search_bitmap);

        menuSearch.setIcon(getResources().getDrawable(R.drawable.search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (managerFragment != null) {
                    managerFragment.filterList(newText);
                }
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconToolbarImageView.setVisibility(View.GONE);
                toolbarTitleTextView.setVisibility(View.GONE);
                //menuRemote.setVisible(false);
                menuSort.setVisible(false);
                //Set cursor in searchView
                searchView.setFocusable(true);
                searchView.requestFocusFromTouch();
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "MainActivity OnCloseListener");
                iconToolbarImageView.setVisibility(View.VISIBLE);
                toolbarTitleTextView.setVisibility(View.VISIBLE);
                //menuRemote.setVisible(true);
                menuSort.setVisible(true);
                if (managerFragment != null) {
                    managerFragment.unfilterList();
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Log.d(TAG, "MainActivity onBackPressed");
        if (managerFragment != null) {
            managerFragment.onBackPressed();
        }
    }

    @Override
    public void onFinishApp() {
        Log.d(TAG, "MainActivity onFinishApp");
        finish();
    }
}

