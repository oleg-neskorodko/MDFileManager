package com.mdgroup.mdfilemanager;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity implements FragmentInteractionListener {

    public static String TAG = "tag";
    private ManagerFragment managerFragment;
    private AboutFragment aboutFragment;
    private Toolbar toolbarTop;
    private SearchView mainSearchView;
    private String currentDirectory;
    private ImageView pasteMainImageView;
    private ImageView sortMainImageView;
    private ImageView searchMainImageView;
    private ImageView folderMainImageView;
    private ImageView fileMainImageView;
    private ImageView infoMainImageView;
    private TextView nameMainTextView;
    private TextView versionMainTextView;
    private ImageView[] toolbarIcon;
    private ImageView[] displayedToolbarIcon;
    private String[] menuNames;
    private String[] overflowMenuNames;
    private int[] menuIndexes;
    private int[] overflowMenuIndex;
    private MenuItem[] menuItems;
    private boolean pasteIconShown;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity onCreate");
        setContentView(R.layout.activity_main);

        menuNames = getResources().getStringArray(R.array.menu_items);
        menuIndexes = new int[]{0, 1, 2, 3, 4, 5};
        pasteIconShown = false;

        initViews();
        setSearchView();
        fillIconList();

        Drawable source = getResources().getDrawable(R.drawable.overflow_menu);
        Bitmap bitmap = ((BitmapDrawable) source).getBitmap();
        Drawable drawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 50, 50, true));
        drawable.setTint(getResources().getColor(R.color.colorWhite));
        toolbarTop.setOverflowIcon(drawable);
        pasteMainImageView.setVisibility(View.INVISIBLE);
        mainSearchView.setVisibility(View.GONE);
        nameMainTextView.setVisibility(View.GONE);
        versionMainTextView.setVisibility(View.GONE);

        setSupportActionBar(toolbarTop);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        managerFragment = new ManagerFragment();
        managerFragment.setListener(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, managerFragment, "manager_fragment").addToBackStack("manager_fragment").commit();
        } else {
            ManagerFragment fragment = (ManagerFragment) getSupportFragmentManager().findFragmentByTag("manager_fragment");
            if (fragment != null) {
                fragment.setListener(this);
            }
        }
        Log.d(TAG, "MainActivity onCreate end");
    }

    private void initViews() {
        toolbarTop = (Toolbar) findViewById(R.id.mainToolbar);
        mainSearchView = toolbarTop.findViewById(R.id.mainSearchView);
        pasteMainImageView = toolbarTop.findViewById(R.id.pasteMainImageView);
        sortMainImageView = toolbarTop.findViewById(R.id.sortMainImageView);
        searchMainImageView = toolbarTop.findViewById(R.id.searchMainImageView);
        folderMainImageView = toolbarTop.findViewById(R.id.folderMainImageView);
        fileMainImageView = toolbarTop.findViewById(R.id.fileMainImageView);
        infoMainImageView = toolbarTop.findViewById(R.id.infoMainImageView);
        nameMainTextView = toolbarTop.findViewById(R.id.nameMainTextView);
        versionMainTextView = toolbarTop.findViewById(R.id.versionMainTextView);
        toolbarIcon = new ImageView[]{pasteMainImageView, sortMainImageView, searchMainImageView, folderMainImageView,
                fileMainImageView, infoMainImageView};

        View.OnClickListener menuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.pasteMainImageView:
                        setPasteIconState(false);
                        if (managerFragment != null) {
                            managerFragment.pasteFileIntoCurrentDirectory();
                        }
                        break;
                    case R.id.sortMainImageView:
                        if (managerFragment != null) {
                            managerFragment.onSortListPressed();
                        }
                        break;
                    case R.id.searchMainImageView:
                        showHideSearch(View.GONE, View.VISIBLE, false);
                        break;
                    case R.id.folderMainImageView:
                        if (managerFragment != null) {
                            managerFragment.createNewFolder();
                        }
                        break;
                    case R.id.fileMainImageView:
                        if (managerFragment != null) {
                            managerFragment.createNewDocument();
                        }
                        break;
                    case R.id.infoMainImageView:
                        showHideInfoToolbar(View.GONE, View.VISIBLE, false);
                        if (managerFragment != null) {
                            currentDirectory = managerFragment.getCurrentDirectory();
                        }
                        aboutFragment = new AboutFragment();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.content_frame, aboutFragment, "about_fragment").addToBackStack("about_fragment").commit();
                        break;
                }
            }
        };

        for (int i = 0; i < toolbarIcon.length; i++) {
            toolbarIcon[i].setOnClickListener(menuClickListener);
        }
    }

    private void setSearchView() {
        mainSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

        mainSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "MainActivity OnCloseListener");
                showHideSearch(View.VISIBLE, View.GONE, true);
                if (managerFragment != null) {
                    managerFragment.unfilterList();
                }
                return false;
            }
        });
    }

    private void fillIconList() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) pasteMainImageView.getLayoutParams();
        int iconWidth = pasteMainImageView.getLayoutParams().width + 2 * lp.rightMargin;
        LinearLayout.LayoutParams lpToolbar = (LinearLayout.LayoutParams) toolbarTop.getLayoutParams();
        int toolbarMargins = lpToolbar.rightMargin + lpToolbar.leftMargin;
        Log.d(TAG, "MainActivity iconWidth = " + iconWidth + " , screen = " + screenWidth);

        int iconNumber = (screenWidth - 10 - toolbarMargins - screenWidth % iconWidth) / iconWidth;
        if (iconNumber < toolbarIcon.length) {
            displayedToolbarIcon = new ImageView[iconNumber];
            overflowMenuNames = new String[menuNames.length - iconNumber];
            overflowMenuIndex = new int[menuIndexes.length - iconNumber];
            for (int i = iconNumber; i < toolbarIcon.length; i++) {
                toolbarIcon[i].setVisibility(View.GONE);
                overflowMenuNames[i - iconNumber] = menuNames[i];
                overflowMenuIndex[i - iconNumber] = menuIndexes[i];
            }
        } else {
            displayedToolbarIcon = new ImageView[toolbarIcon.length];
        }
        for (int i = 0; i < displayedToolbarIcon.length; i++) {
            displayedToolbarIcon[i] = toolbarIcon[i];
        }
    }

    private void showHideSearch(int buttonsState, int searchState, boolean itemsVisible) {
        for (int i = 0; i < displayedToolbarIcon.length; i++) {
            displayedToolbarIcon[i].setVisibility(buttonsState);
        }
        if (menuItems != null) {
            for (int i = 0; i < menuItems.length; i++) {
                menuItems[i].setVisible(itemsVisible);
            }
        }
        mainSearchView.setVisibility(searchState);
        if (!itemsVisible) {
            mainSearchView.setIconified(false);
        }
        if (itemsVisible && !pasteIconShown) {
            pasteMainImageView.setVisibility(View.INVISIBLE);
        }
    }

    private void showHideInfoToolbar(int buttonsState, int textState, boolean itemsVisible) {
        for (int i = 0; i < displayedToolbarIcon.length; i++) {
            displayedToolbarIcon[i].setVisibility(buttonsState);
        }
        if (menuItems != null) {
            for (int i = 0; i < menuItems.length; i++) {
                menuItems[i].setVisible(itemsVisible);
            }
        }
        nameMainTextView.setVisibility(textState);
        versionMainTextView.setVisibility(textState);
        if (itemsVisible && !pasteIconShown) {
            pasteMainImageView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (overflowMenuNames != null && overflowMenuNames.length > 0) {
            menuItems = new MenuItem[overflowMenuNames.length];
            for (int i = 0; i < overflowMenuNames.length; i++) {
                menuItems[i] = menu.add(Menu.NONE, overflowMenuIndex[i], i, overflowMenuNames[i]);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case 0:
                setPasteIconState(false);
                if (managerFragment != null) {
                    managerFragment.pasteFileIntoCurrentDirectory();
                }
                break;
            case 1:
                if (managerFragment != null) {
                    managerFragment.onSortListPressed();
                }
                break;
            case 2:
                showHideSearch(View.GONE, View.VISIBLE, false);
                break;
            case 3:
                if (managerFragment != null) {
                    managerFragment.createNewFolder();
                }
                break;
            case 4:
                if (managerFragment != null) {
                    managerFragment.createNewDocument();
                }
                break;
            case 5:
                showHideInfoToolbar(View.GONE, View.VISIBLE, false);
                if (managerFragment != null) {
                    currentDirectory = managerFragment.getCurrentDirectory();
                }
                aboutFragment = new AboutFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, aboutFragment, "about_fragment").addToBackStack("about_fragment").commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setPasteIconState(boolean visible) {
        if (visible) {
            pasteMainImageView.setVisibility(View.VISIBLE);
            pasteIconShown = true;
        } else {
            pasteMainImageView.setVisibility(View.INVISIBLE);
            pasteIconShown = false;
        }
    }

    public void onInfoClose() {
        Log.d(TAG, "MainActivity onInfoClose");
        showHideInfoToolbar(View.VISIBLE, View.GONE, true);
    }

    @Override
    public void setCurrentDirectory() {
        managerFragment.setCurrentDirectory(currentDirectory);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "MainActivity onBackPressed");
        FragmentManager fm = getSupportFragmentManager();
        String entryName = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
        Log.d(TAG, "MainActivity backstack fragment = " + entryName);
        if (entryName.equals("about_fragment")) {
            onInfoClose();
        }
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

