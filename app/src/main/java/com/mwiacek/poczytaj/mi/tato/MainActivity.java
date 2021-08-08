package com.mwiacek.poczytaj.mi.tato;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.File;
import java.util.ArrayList;

import static com.mwiacek.poczytaj.mi.tato.ImageCacheForListView.getDiskCacheFolder;

public class MainActivity extends Activity {
    private Activity mActitity;
    private ArrayAdapter<String> adapter;
    private AutoCompleteTextView mSearchTextView;
    private BooksListListViewAdapter customAdapter;
    private BookListListViewAdapter customAdapter2;
    private Button mBackButton;
    private Button mSearchButton;
    private ViewSwitcher mViewSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mActitity = this;

        ImageCacheForListView mImageCache = new ImageCacheForListView(this.getBaseContext());

        mBackButton = findViewById(R.id.backButton);
        ListView mBooksListListView = findViewById(R.id.booksListListView);
        ListView mBooksList2ListView = findViewById(R.id.bookListListView);
        ProgressBar mProgressBar = findViewById(R.id.progressBar);
        mSearchButton = findViewById(R.id.searchButton);
        mSearchTextView = findViewById(R.id.searchTextView);
        mViewSwitcher = findViewById(R.id.viewSwitcher);
        final TextView titleTextView = findViewById(R.id.titleTextView);

        customAdapter = new BooksListListViewAdapter(mImageCache,
                mSearchButton,
                mProgressBar, mSearchTextView);
        customAdapter2 = new BookListListViewAdapter(mImageCache);

        mBooksListListView.setAdapter(customAdapter);
        mBooksListListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Books books = (Books) parent.getAdapter().getItem(position);
                Book book = books.items.get(books.positionInMainList);
                if (books.items.size() != 1) {
                    customAdapter2.BookListListViewAdapterDisplay(books.items);
                    titleTextView.setText(book.volumeInfo.title);
                    mViewSwitcher.showNext();
                    return;
                }
                (new MyCallbackClass()).downloadFileWithDownloadManager(book.downloadUrl,
                        book.volumeInfo.title);
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchTextView.getWindowToken(), 0);

                mSearchButton.setEnabled(false);
                mSearchTextView.setEnabled(false);

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                ArrayList<String> l = new ArrayList<>();
                String s;
                l.add(mSearchTextView.getText().toString());
                for (int i = 0; i < 10; i++) {
                    s = sharedPref.getString("history" + i, "");
                    if (!s.isEmpty() && !s.equals(mSearchTextView.getText().toString())) {
                        l.add(s);
                    }
                }
                if (l.size() > 10) {
                    l.remove(0);
                }
                SharedPreferences.Editor editor = sharedPref.edit();
                for (int i = 0; i < l.size(); i++) {
                    editor.putString("history" + i, l.get(i));
                }
                editor.commit();
                adapter = new ArrayAdapter<>(mActitity, android.R.layout.simple_dropdown_item_1line, l);
                mSearchTextView.setAdapter(adapter);

                customAdapter.BooksListListViewAdapterSearch(
                        mSearchTextView.getText().toString(),
                        getApplicationContext(),
                        sharedPref);
            }
        });

        mBooksList2ListView.setAdapter(customAdapter2);
        mBooksList2ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Book book = (Book) parent.getAdapter().getItem(position);

                (new MyCallbackClass()).downloadFileWithDownloadManager(book.downloadUrl,
                        book.volumeInfo.title);
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewSwitcher.showPrevious();
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ArrayList<String> l = new ArrayList<>();
        String s;
        for (int i = 0; i < 10; i++) {
            s = sharedPref.getString("history" + i, "");
            if (!s.isEmpty()) {
                l.add(s);
            }
        }
        adapter = new ArrayAdapter<>(mActitity, android.R.layout.simple_dropdown_item_1line, l);
        mSearchTextView.setAdapter(adapter);
        mSearchTextView.setThreshold(0);

       // mSearchTextView.setText("warszawo naprzód");
       // mSearchButton.callOnClick();

        mSearchTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View paramView, MotionEvent paramMotionEvent) {
                adapter.getFilter().filter(null);
                mSearchTextView.showDropDown();
                return false;
            }
        });

        mSearchTextView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER) {
                    mSearchTextView.dismissDropDown();
                    mSearchButton.callOnClick();
                    return true;
                }
                return false;
            }
        });

        mSearchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSearchButton.callOnClick();
            }
        });

        mSearchTextView.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                adapter.getFilter().filter(null);
                if (mSearchTextView.isShown()) {
                    mSearchTextView.showDropDown();
                }
                mSearchButton.setEnabled(mSearchTextView.length() != 0);
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    File dirList = new File(getDiskCacheFolder(getApplicationContext()));
                    File[] files = dirList.listFiles();
                    if (files.length < 500) {
                        break;
                    }
                    long lmod = java.lang.Long.MAX_VALUE;
                    File f = null;
                    for (File file : files) {
                        if (file.lastModified() < lmod) {
                            lmod = file.lastModified();
                            f = file;
                        }
                    }
                    if (lmod != java.lang.Long.MAX_VALUE) {
                        f.delete();
                    }
                }
            }
        };

        (new Thread(r)).start();
    }

    public void onBackPressed() {
        if (mBackButton.isShown()) {
            mViewSwitcher.showPrevious();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sett:
                Intent intent = new Intent(mActitity, PreferencesActivity.class);
                startActivityForResult(intent, 0);
                //todo: reading list of engines?
                return true;
            case R.id.report:
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                String subject = "";
                try {
                    subject = "Poczytaj mi tato " +
                            getPackageManager().getPackageInfo(getPackageName(), 0).versionName +
                            " / Android " +
                            Build.VERSION.RELEASE;
                } catch (Exception ignored) {
                }
                String[] extra = new String[]{"marcin@mwiacek.com"};
                emailIntent.putExtra(Intent.EXTRA_EMAIL, extra);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.setType("message/rfc822");
                try {
                    startActivity(emailIntent);
                } catch (Exception e) {
                    AlertDialog alertDialog;
                    alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle("Informacja");
                    alertDialog.setMessage("Błąd stworzenia maila");
                    alertDialog.show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class MyCallbackClass {
        public void downloadFileWithDownloadManager(String url, String title) {
            if (url.length() == 0) {
                return;
            }

            if (url.contains(".htm") || url.contains("artrage.pl")) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(mActitity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
                    return;
                }
            }
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);

            DownloadManager downloadmanager = (DownloadManager) getSystemService(android.content.Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);

            File f = new File("" + uri);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(f.getName());
            request.setDescription(title);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.parse("file://" + path + "/" + f.getName()));
            downloadmanager.enqueue(request);
        }
    }

}
