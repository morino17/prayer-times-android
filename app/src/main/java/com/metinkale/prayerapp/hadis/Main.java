package com.metinkale.prayerapp.hadis;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.metinkale.prayer.R;
import com.metinkale.prayerapp.App;
import com.metinkale.prayerapp.BaseActivity;
import com.metinkale.prayerapp.custom.NumberDialog;
import com.metinkale.prayerapp.custom.NumberDialog.OnNumberChangeListener;
import com.metinkale.prayerapp.settings.Prefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Main extends BaseActivity implements OnClickListener, OnQueryTextListener {

    private static final int STATE_SHUFFLED = 0, STATE_ORDER = 1, STATE_FAVORITE = 2;

    private int mState;
    private ViewPager mPager;
    private MyAdapter mAdapter;
    private TextView mNumber;
    private ImageView mLeft, mRight;
    private SharedPreferences mPrefs;
    private MenuItem mSwitch, mFav;
    private List<Integer> mFavs = new ArrayList<>();
    private List<Integer> mList = new ArrayList<>();
    private int mRemFav = -1;
    private ShareActionProvider mShareActionProvider;
    private SearchTask mTask;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hadis_main);

        mPrefs = getSharedPreferences("hadis", 0);
        mNumber = (TextView) findViewById(R.id.number);
        mLeft = (ImageView) findViewById(R.id.left);
        mRight = (ImageView) findViewById(R.id.right);
        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mLeft.setOnClickListener(this);
        mRight.setOnClickListener(this);
        mNumber.setOnClickListener(this);

        loadFavs();
        try {
            setState(STATE_SHUFFLED);
        } catch (RuntimeException e) {
            finish();
            new File(App.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), Prefs.getLanguage() + "/hadis.db").delete();
            startActivity(new Intent(this, com.metinkale.prayerapp.vakit.Main.class));
        }
    }

    private boolean setState(int state) {
        mList.clear();
        if (mQuery == null) {
            mPrefs.edit().putInt(last(), mPager.getCurrentItem()).apply();
        }
        mQuery = null;

        switch (state) {
            case STATE_ORDER:

                for (int i = 1; i <= Shuffled.getList().size(); i++) {
                    mList.add(i);
                }
                break;
            case STATE_SHUFFLED:
                mList.addAll(Shuffled.getList());
                break;
            case STATE_FAVORITE:
                mList.addAll(mFavs);
                break;
            default:
                mList.addAll(SqliteHelper.get().get(SqliteHelper.get().getCategories().get(state - 3)));
                break;
        }
        if (mList.isEmpty()) {
            setState(mState);
            return false;

        }

        mState = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                mPager.setCurrentItem(9999);
                mPager.setAdapter(mAdapter);
                mPager.setCurrentItem(mPrefs.getInt(last(), 0));
            }
        });

        return true;

    }

    @Override
    protected void onResume() {
        super.onResume();
        mPager.setCurrentItem(mPrefs.getInt(last(), 0));
    }

    private String last() {
        if (mState == STATE_FAVORITE || mState == STATE_SHUFFLED || mState == STATE_ORDER) {
            return "last_nr" + (mState == STATE_FAVORITE) + (mState == STATE_SHUFFLED);
        } else {
            return "last_nr" + mState;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.edit().putInt(last(), mPager.getCurrentItem()).apply();
    }

    @Override
    public void onClick(View v) {
        if (v == mLeft) {
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        } else if (v == mRight) {
            mPager.setCurrentItem(mPager.getCurrentItem() + 1);
        } else if (v == mNumber) {
            NumberDialog nd = NumberDialog.create(1, mAdapter.getCount() + 1, mPager.getCurrentItem() + 1);
            nd.setOnNumberChangeListener(new OnNumberChangeListener() {
                @Override
                public void onNumberChange(int nr) {
                    mPager.setCurrentItem(nr - 1, false);
                }
            });
            nd.show(getSupportFragmentManager(), null);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == mFav.getItemId()) {
            int i = (int) mAdapter.getItemId(mPager.getCurrentItem());
            if (mState == STATE_FAVORITE) {
                if (mRemFav == -1) {
                    mFav.setIcon(R.drawable.ic_action_nofav);
                    mRemFav = i;
                    mNumber.setText(mPager.getCurrentItem() + 1 + "/" + (mAdapter.getCount() - 1));
                } else {
                    mFav.setIcon(R.drawable.ic_action_favorite);
                    mRemFav = -1;
                    mNumber.setText(mPager.getCurrentItem() + 1 + "/" + mAdapter.getCount());

                }
            } else {
                if (mFavs.contains(i)) {
                    mFavs.remove((Integer) i);
                } else {
                    mFavs.add(i);
                }
                mAdapter.notifyDataSetChanged();
                setCurrentPage(mPager.getCurrentItem());
                storeFavs();
            }

        } else if (item.getItemId() == mSwitch.getItemId()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            List<String> cats = SqliteHelper.get().getCategories();
            List<String> items = new ArrayList<>();
            items.add(getString(R.string.mixed));
            items.add(getString(R.string.sorted));
            items.add(getString(R.string.favorite));
            for (String cat : cats) {
                items.add(Html.fromHtml(cat).toString());
            }
            builder.setTitle(items.get(mState)).setItems(items.toArray(new CharSequence[items.size()]), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!setState(which)) {
                        Toast.makeText(Main.this, R.string.noFavs, Toast.LENGTH_LONG).show();
                    }

                }

            });
            builder.show();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.hadis, menu);

        mSwitch = menu.findItem(R.id.favswitch);
        mFav = menu.findItem(R.id.fav);
        setCurrentPage(mPager.getCurrentItem());

        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        item = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) item.getActionView();

        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onQueryTextSubmit("");
    }

    void setCurrentPage(int i) {
        if (i >= mAdapter.getCount())
            i = mAdapter.getCount() - 1;

        mNumber.setText(i + 1 + "/" + mAdapter.getCount());
        if (mFav == null) {
            return;
        }
        if (mFavs.contains((int) mAdapter.getItemId(mPager.getCurrentItem()))) {
            mFav.setIcon(R.drawable.ic_action_favorite);
        } else {
            mFav.setIcon(R.drawable.ic_action_nofav);
        }

        if (mRemFav != -1 && mFavs.contains(mRemFav)) {
            mFavs.remove((Integer) mRemFav);

            mAdapter.notifyDataSetChanged();
            setCurrentPage(mPager.getCurrentItem());
            storeFavs();
            mRemFav = -1;
        }
    }

    void storeFavs() {
        Collections.sort(mFavs, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return b - a;
            }
        });
        SharedPreferences.Editor edit = getSharedPreferences("hadis", Context.MODE_PRIVATE).edit();
        edit.clear();
        edit.putInt("Count", mFavs.size());
        int count = 0;
        for (int i : mFavs) {
            edit.putInt("fav_" + count++, i);
        }
        edit.apply();
    }

    void loadFavs() {
        SharedPreferences prefs = getSharedPreferences("hadis", Context.MODE_PRIVATE);
        int count = prefs.getInt("Count", 0);

        for (int i = 0; i < count; i++) {
            mFavs.add(prefs.getInt("fav_" + i, i) + 1);
        }

    }

    private void setShareText(String txt) {
        txt = txt.replace("\n", "|");
        if (mShareActionProvider != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(txt).toString().replace("|", "\n"));
            shareIntent.setType("text/plain");
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mTask != null && mTask.getStatus() == Status.RUNNING) {
            return false;
        }

        mQuery = query;
        mTask = new SearchTask(this);
        mTask.execute(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean setNavBar() {
        this.setNavBarColor(0xaa333333);
        return true;
    }

    public class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public long getItemId(int pos) {
            return mList.get(pos);

        }

        @Override
        public Fragment getItem(int pos) {
            return Frag.create((int) getItemId(pos));
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            setCurrentPage(position);

            if (object instanceof Frag) {
                ((Frag) object).setQuery(mQuery);
                String hadis = ((Fragment) object).getArguments().getString("hadis");
                String kaynak = ((Fragment) object).getArguments().getString("kaynak");
                setShareText(hadis + (kaynak.length() <= 3 ? "" : "\n\n" + kaynak));
            }
        }
    }

    public class SearchTask extends AsyncTask<String, String, Boolean> {
        private ProgressDialog dialog;

        public SearchTask(Context c) {
            dialog = new ProgressDialog(c);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View v = getCurrentFocus();

            if (v != null) {
                inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            v.clearFocus();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (!isCancelled()) {

                mAdapter.notifyDataSetChanged();
                mPager.setCurrentItem(9999);
                mPager.setAdapter(mAdapter);
                mPager.setCurrentItem(0);
            }

            int s = mList.size();
            Toast.makeText(Main.this, getString(R.string.foundXHadis, s == SqliteHelper.get().getCount() ? 0 : s), Toast.LENGTH_LONG).show();

        }

        @Override
        protected void onProgressUpdate(String... arg) {
            dialog.setMessage(arg[0]);
        }

        @Override
        protected Boolean doInBackground(String... args) {
            if (args[0].equals("")) {
                return false;
            }
            List<Integer> q;
            q = SqliteHelper.get().search(args[0]);
            if (!q.isEmpty()) {
                mList = q;
            }

            return !q.isEmpty();
        }

    }

}