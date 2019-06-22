package com.mdgroup.mdfilemanager;

import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements FragmentInteractionListener{

    public static String TAG = "tag";
    private ManagerFragment managerFragment;
    private AboutFragment aboutFragment;
    private Toolbar toolbarTop;
    private Toolbar searchToolbar;
    private Menu mainMenu;
    //private ImageView iconToolbarImageView;
    //private TextView toolbarTitleTextView;
    private android.support.v7.widget.SearchView mainSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate");
        setContentView(R.layout.activity_main);
        //Log.d(TAG, "MainActivity setContentView");

        toolbarTop = (Toolbar) findViewById(R.id.mainToolbar);

        //iconToolbarImageView = toolbarTop.findViewById(R.id.iconToolbarImageView);
        //toolbarTitleTextView = toolbarTop.findViewById(R.id.toolbarTitleTextView);
        //toolbarTitleTextView.setVisibility(View.GONE);

        setSupportActionBar(toolbarTop);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

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
        mainMenu = menu;
        createSearch(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_info:
                aboutFragment = new AboutFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, aboutFragment, "about_fragment").addToBackStack("main_stack").commit();
                break;
            case R.id.menu_sort:
                if (managerFragment != null) {
                    managerFragment.onSortListPressed();
                }
                break;
            case R.id.menu_search:
                break;
            case R.id.menu_paste:
                setPasteIconState(false);
                if (managerFragment != null) {
                    managerFragment.pasteFileIntoCurrentDirectory();
                }
                break;
            case R.id.new_folder:
                if (managerFragment != null) {
                    managerFragment.createNewFolder();
                }
                break;
            case R.id.new_file:
                if (managerFragment != null) {
                    managerFragment.createNewDocument();
                }
                break;
            case R.id.menu_settings:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setPasteIconState (boolean visible) {
        MenuItem menuPaste = mainMenu.findItem(R.id.menu_paste);
        menuPaste.setVisible(visible);
    }

    private void createSearch(Menu menu) {
        final MenuItem menuSort = menu.findItem(R.id.menu_sort);
        final MenuItem menuInfo = menu.findItem(R.id.menu_info);
        final MenuItem menuSearch = menu.findItem(R.id.menu_search);
        final MenuItem menuPaste = menu.findItem(R.id.menu_paste);
        final MenuItem newFolder = menu.findItem(R.id.new_folder);
        final MenuItem newFile = menu.findItem(R.id.new_file);
        final MenuItem menuSettings = menu.findItem(R.id.menu_settings);


        menuPaste.setVisible(false);

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
                //iconToolbarImageView.setVisibility(View.GONE);
                //toolbarTitleTextView.setVisibility(View.GONE);
                //menuRemote.setVisible(false);
                menuSort.setVisible(false);
                menuInfo.setVisible(false);
                newFolder.setVisible(false);
                //Set cursor in searchView
                searchView.setFocusable(true);
                searchView.requestFocusFromTouch();
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "MainActivity OnCloseListener");
                //iconToolbarImageView.setVisibility(View.VISIBLE);
                //toolbarTitleTextView.setVisibility(View.VISIBLE);
                //menuRemote.setVisible(true);
                menuSort.setVisible(true);
                menuInfo.setVisible(true);
                newFolder.setVisible(true);
                if (managerFragment != null) {
                    managerFragment.unfilterList();
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "MainActivity onBackPressed");
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() == 1) {
            if (managerFragment != null) {
                managerFragment.onBackPressed();
            }
        } else super.onBackPressed();
    }

    @Override
    public void onFinishApp() {
        Log.d(TAG, "MainActivity onFinishApp");
        finish();
    }
}

