package com.mwiacek.poczytaj.mi.tato;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.HttpsURLConnection;

public class BooksListListViewAdapter extends BaseAdapter {
    private final ImageCacheForListView mImageCache;
    private final ArrayList<Books> mData = new ArrayList<>();
    private final ArrayList<StoreInfo> mList = new ArrayList<>();
    private final Button mSearchButton;
    private final AutoCompleteTextView mSearchTextView;
    private final ProgressBar mProgressBar;
    private final ReentrantLock lock = new ReentrantLock();
    private String mSearchText;
    private int pageNumber;
    private int sitesWithContent;
    private int sitesProcessed;

    BooksListListViewAdapter(ImageCacheForListView imageCache,
                             Button searchButton, ProgressBar progressBar,
                             AutoCompleteTextView searchTextView) {
        mImageCache = imageCache;
        mSearchButton = searchButton;
        mProgressBar = progressBar;
        mSearchTextView = searchTextView;
    }


    @SuppressWarnings("deprecation")
    private String stripHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    private String findBetween(String s, String start, String stop, int startIndex) {
        if (startIndex == -1) {
            return "";
        }
        int fromPosition = s.indexOf(start, startIndex);
        if (fromPosition == -1) {
            return "";
        }
        int toPosition = s.indexOf(stop, fromPosition + start.length());
        if (toPosition == -1) {
            return "";
        }
        return s.substring(fromPosition + start.length(), toPosition);
    }

    /**
     * Method for starting books search
     */
    public void BooksListListViewAdapterSearch(String searchText, Context context,
                                               SharedPreferences sharedPref) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(context, "Mistrzu, lepiej działa z siecią!", Toast.LENGTH_SHORT)
                    .show();
            mSearchButton.setEnabled(true);
            mSearchTextView.setEnabled(true);
            return;
        }

        if (mData.size() != 0) {
            mData.clear();
            notifyDataSetInvalidated();
        }

        mList.clear();
        if (sharedPref.getBoolean("wolnelektury.pl", true)) {
            mList.add(new StoreInfoWolneLektury());
        }
        if (sharedPref.getBoolean("ebooki.swiatczytnikow.pl", true)) {
            mList.add(new StoreInfoEbookiSwiatCzytnikow());
        }
        if (sharedPref.getBoolean("upolujebooka.pl", true)) {
            mList.add(new UpolujEbookaSwiatCzytnikow());
        }
        if (sharedPref.getBoolean("bookto.pl", true)) {
            mList.add(new BooktoSwiatCzytnikow());
        }
        if (sharedPref.getBoolean("bookrage.org", true)) {
            mList.add(new BookRage());
        }
        if (sharedPref.getBoolean("ibuk.pl", true)) {
            mList.add(new IBUK());
        }
        if (mList.size() == 0) {
            Toast.makeText(context, "Sorry Gregory, nie wybrano żadnej wyszukiwarki w opcjach!", Toast.LENGTH_SHORT)
                    .show();
            mSearchButton.setEnabled(true);
            mSearchTextView.setEnabled(true);
            return;
        }

        mSearchText = searchText;

        pageNumber = 1;
        sitesWithContent = 0;
        sitesProcessed = 0;
        Iterator<StoreInfo> itr = mList.iterator();
        StoreInfo ele;
        while (itr.hasNext()) {
            ele = itr.next();
            new DownloadBooksInfoTask(this, mSearchButton, mProgressBar)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, searchText, ele, mData, 100 / mList.size());
        }
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageCacheForListView.ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.book_list_item, parent, false);

            holder = new ImageCacheForListView.ViewHolder();
            holder.titleText = convertView.findViewById(R.id.BookName);
            holder.authorText = convertView.findViewById(R.id.BookAuthor);
            holder.priceText = convertView.findViewById(R.id.BookPrice);
            holder.thumbnailPicture = convertView.findViewById(R.id.BookImage);

            convertView.setTag(holder);
        } else {
            holder = (ImageCacheForListView.ViewHolder) convertView.getTag();
        }

        // Read next page if we have last entry and something to read
        if (position == getCount() - 1 && pageNumber != -1 && mSearchButton.isEnabled()) {

            //TODO: reading list of engines from settings

            pageNumber++;
            sitesWithContent = 0;
            sitesProcessed = 0;

            mSearchButton.setEnabled(false);
            mSearchTextView.setEnabled(false);

            Iterator<StoreInfo> itr = mList.iterator();
            StoreInfo ele;
            while (itr.hasNext()) {
                ele = itr.next();
                new DownloadBooksInfoTask(this, mSearchButton, mProgressBar)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                mSearchText,
                                ele,
                                mData,
                                100 / mList.size());
            }
        }

        Book b = ((Books) getItem(position)).items.get(((Books) getItem(position)).positionInMainList);

        holder.position = position;
        holder.titleText.setText(b.volumeInfo.title);
        String s = "";
        if (b.downloadUrl.contains(".epub") && ((Books) getItem(position)).items.size() == 1) {
            s = "Pobierz";
        } else {
            if (b.price != 0.0) {
                s = ((Books) getItem(position)).items.size() != 1 ?
                        "Od " + String.valueOf(b.price) + " PLN" : String.valueOf(b.price) + " PLN";
            } else {
                s = ((Books) getItem(position)).items.size() != 1 ?
                        "Od 0.00 PLN" : "Darmowa";
            }
        }
        if (b.offerExpiryDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d.M");
            s = s + " do " + dateFormat.format(b.offerExpiryDate);
        }
        holder.priceText.setText(s);
        if (b.volumeInfo.authors != null) {
            holder.authorText.setText(b.volumeInfo.authors[0]);
        }

        mImageCache.getImageFromCache(convertView.getContext(),
                b.volumeInfo.smallThumbnail, holder, position);

        return convertView;
    }

    public abstract class StoreInfo {
        public abstract boolean doesItMatch(String name, String url,
                                            StringBuilder pageContent, ArrayList<Books> books);

        public abstract String[] getSearchUrl(String name);

        public boolean addBook(Book book, ArrayList<Books> allBooks, int sortOrder) {
            Book b2;
            lock.lock();
            try {
                for (Books books : allBooks) {
                    for (Book b : books.items) {
                        if (book.volumeInfo.title.equalsIgnoreCase(b.volumeInfo.title) &&
                                book.volumeInfo.authors[0].equalsIgnoreCase(b.volumeInfo.authors[0]) &&
                                book.downloadUrl.equals(b.downloadUrl)) {
                            return false;
                        }
                    }
                    b2 = books.items.get(books.positionInMainList);
                    if (book.volumeInfo.title.equalsIgnoreCase(b2.volumeInfo.title) &&
                            book.volumeInfo.authors[0].equalsIgnoreCase(b2.volumeInfo.authors[0])) {
                        books.items.add(book);
                        if (book.price < b2.price || book.downloadUrl.contains(".epub")) {
                            books.positionInMainList = books.items.size() - 1;
                            //books.sortOrderInMainList = sortOrder;
                        }

                        return true;
                    }

                }
                Books nb = new Books();
                nb.items = new ArrayList<>();
                nb.items.add(book);
                nb.positionInMainList = 0;
                //nb.sortOrderInMainList = sortOrder;
                allBooks.add(nb);
                return true;
            } finally {
                lock.unlock();
            }
        }
    }

    public class StoreInfoWolneLektury extends StoreInfo {
        public String[] getSearchUrl(String name) {
            String[] urls = {"https://wolnelektury.pl/szukaj/?q=" + name};
            return urls;
        }

        public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books) {
            String formattedName = name.toLowerCase().replaceAll("\\s", "-");
            formattedName = Normalizer.normalize(formattedName, Normalizer.Form.NFD);
            formattedName = formattedName.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            int startSearchPosition, fromPosition, toPosition, sortOrder = 1;
            Book book;
            String s;

            fromPosition = pageContent.indexOf("<div class=\"book-box-inner\"><p>Znalezione w treści</p></div>");
            if (fromPosition != -1) {
                toPosition = pageContent.length();
                pageContent.delete(fromPosition, toPosition);
            }
            toPosition = 0;
            while (true) {
                startSearchPosition = toPosition;
                fromPosition = pageContent.indexOf("<div class=\"book-box-inner\">",
                        startSearchPosition);
                if (fromPosition == -1) {
                    break;
                }
                toPosition = pageContent.indexOf("</div></li>", fromPosition);
                s = pageContent.substring(fromPosition, toPosition);

                book = new Book();

                book.offerExpiryDate = null;

                book.price = 0;
                book.volumeInfo = new VolumeInfo();
                book.volumeInfo.title =
                        stripHtml(findBetween(s, "<div class=\"title\">", "</div>", 0))
                                .trim();
                book.volumeInfo.authors = new String[1];
                book.volumeInfo.authors[0] = findBetween(s, "/\">", "</a>",
                        s.indexOf("<div class=\"author\">"));
                book.volumeInfo.smallThumbnail = "https://wolnelektury.pl" +
                        findBetween(s, "<img src=\"", "\" alt=\"Cover\"", 0);
                book.downloadUrl = "https://wolnelektury.pl/media/book/epub/" +
                        formattedName + ".epub";

                if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                        book.volumeInfo.title.isEmpty()) {
                    break;
                }
                addBook(book, books, sortOrder);
                sortOrder++;
            }
            return false;
        }
    }

    public class StoreInfoEbookiSwiatCzytnikow extends StoreInfo {
        public String[] getSearchUrl(String name) {
            //popularne
            String[] urls = {"https://ebooki.swiatczytnikow.pl/szukaj/" + name + "?strona=" + pageNumber};
            return urls;
        }

        public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books) {
            int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
            String s, s2;
            Book book;
            Boolean added = false;

            while (true) {
                startSearchPosition = toPosition;
                fromPosition = pageContent.indexOf("<li class=\"resultbox\">",
                        startSearchPosition);
                if (fromPosition == -1) {
                    break;
                }
                toPosition = pageContent.indexOf("</li>", fromPosition);
                s = pageContent.substring(fromPosition, toPosition);

                if (s.contains("Brak ofert")) {
                    continue;
                }

                book = new Book();

                book.offerExpiryDate = null;

                book.volumeInfo = new VolumeInfo();
                book.volumeInfo.smallThumbnail = "https://" + findBetween(s, "<img src=\"", "\"", 0);

                s2 = findBetween(s, "<div class=\"title\">", "</div>", 0);
                book.volumeInfo.title = findBetween(s2, "\">", "</a>", s2.indexOf("<a href=\""));
                book.downloadUrl = "https://ebooki.swiatczytnikow.pl/" +
                        findBetween(s2, "<a href=\"", "\">", 0);

                book.volumeInfo.authors = new String[1];
                book.volumeInfo.authors[0] =
                        stripHtml(findBetween(s, "<div class=\"author\">", "</div>", 0))
                                .trim();

                if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                        book.volumeInfo.title.isEmpty()) {
                    break;
                }

                s2 = findBetween(s, "<strong>", "</strong>", 0);
                if (s2.equals("")) {
                    s2 = findBetween(s, "<strong id=\"\">", "</strong>", 0);
                }
                if (s2.equals("")) {
                    book.price = (float) 0.0;
                } else {
                    book.price = Float.valueOf(s2.replace(",", "."));
                }

                if (addBook(book, books, sortOrder)) {
                    added = true;
                }
                sortOrder++;
            }
            return (pageContent.indexOf("\">następna »</a></div>") != -1 &&
                    added);
        }
    }

    public class UpolujEbookaSwiatCzytnikow extends StoreInfo {
        public String[] getSearchUrl(String name) {
            //najpopularniejsze
            String[] urls = {"https://upolujebooka.pl/szukaj," + pageNumber + "," + name + ".html?order_by=1"};
            return urls;
        }

        public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books) {
            int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
            String s, s2;
            Book book;
            Boolean added = false;
            while (true) {
                startSearchPosition = toPosition;
                fromPosition = pageContent.indexOf("<div class=\"item\">",
                        startSearchPosition);
                if (fromPosition == -1) {
                    break;
                }
                toPosition = pageContent.indexOf("<span class=\"priceMax\">", fromPosition);
                s = pageContent.substring(fromPosition, toPosition);

                book = new Book();
                book.offerExpiryDate = null;

                book.volumeInfo = new VolumeInfo();
                book.volumeInfo.smallThumbnail = "https://upolujebooka.pl/" +
                        findBetween(s, "itemprop=\"image\" src=\"", "\"", 0);

                book.volumeInfo.title = findBetween(s, "<h2 itemprop=\"name\">", "</h2>", 0);
                book.downloadUrl = "https://upolujebooka.pl/" +
                        findBetween(s, "<meta itemprop=\"url\" content=\"", "\"", 0);

                book.volumeInfo.authors = new String[1];
                book.volumeInfo.authors[0] =
                        stripHtml(findBetween(s, "itemprop=\"author\"  >", "</a>", 0))
                                .trim();


                s2 = findBetween(s, "<span itemprop=\"price\">", "</span>", 0);
                if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                        book.volumeInfo.title.isEmpty() || s2.isEmpty()) {
                    break;
                }
                book.price = Float.valueOf(s2);

                if (addBook(book, books, sortOrder)) {
                    added = true;
                }
                sortOrder++;
            }
            return (pageContent.indexOf("rel=\"next\">następna</a></li>") != -1) &&
                    added;
        }
    }

    public class BooktoSwiatCzytnikow extends StoreInfo {
        public String[] getSearchUrl(String name) {
            String[] urls = {"http://bookto.pl/szukaj/" + name + "/t-e0/p-c0/" + pageNumber};
            return urls;
        }

        public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books) {

            int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
            String s, s2;
            Book book;
            Boolean added = false;
            while (true) {
                startSearchPosition = toPosition;
                fromPosition = pageContent.indexOf("<div class=\"cover\">",
                        startSearchPosition);
                if (fromPosition == -1) {
                    break;
                }
                toPosition = pageContent.indexOf("</div></div></div></div></div>", fromPosition);
                s = pageContent.substring(fromPosition, toPosition);

                book = new Book();
                book.offerExpiryDate = null;

                book.volumeInfo = new VolumeInfo();
                book.volumeInfo.smallThumbnail =
                        findBetween(s, "<img src=\"", "\"", 0);

                book.volumeInfo.title = findBetween(s, "<span itemprop=\"name\">", "</span>", 0);

                book.downloadUrl =
                        findBetween(s, "<h3 class=\"title\"><a href=\"", "\"", 0) + ".htm";

                book.volumeInfo.authors = new String[1];
                book.volumeInfo.authors[0] =
                        stripHtml(
                                findBetween(s, "<span itemprop=\"name\">", "</span>",
                                        s.indexOf("<span itemprop=\"author\"")))
                                .trim();
                s2 = findBetween(s, "<meta itemprop=\"price\" content=\"", "\"", 0);
                if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                        book.volumeInfo.title.isEmpty() || s2.isEmpty()) {
                    break;
                }

                book.price = Float.valueOf(s2);

                if (addBook(book, books, sortOrder)) {
                    added = true;
                }
                sortOrder++;
            }
            return (pageContent.indexOf("<i class=\"fa fa-angle-right\"></i>") != -1) &&
                    added;
        }
    }

    public class BookRage extends StoreInfo {
        public String[] getSearchUrl(String name) {
            String[] urls = {"https://artrage.pl/bookrage", "https://artrage.pl/bookrage/quick"};
            return urls;
        }

        public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books) {
            int startSearchPosition, fromPosition, fromPosition2, toPosition = 0, sortOrder = 1;
            String s, s2;
            Book book;
            Boolean added = false;
            Date expiryDate;
            Float price = (float) 0;

            s = findBetween(pageContent.toString(), "<p data-ends-at=\"", "\"", 0);
            if (s.equals("")) return false;

            SimpleDateFormat format = new SimpleDateFormat("y/M/d H:m");
            try {
                expiryDate = format.parse(s);
            } catch (ParseException e) {
                return false;
            }

            while (true) {
                startSearchPosition = toPosition;

                fromPosition = pageContent.indexOf("<article class=\"book\"",
                        startSearchPosition);
                if (fromPosition == -1) {
                    break;
                }
                fromPosition2 = pageContent.indexOf("<h5",
                        startSearchPosition);

                if (fromPosition2 != -1 && fromPosition2 < fromPosition) {
                    toPosition = pageContent.indexOf("</h5>", fromPosition2);
                    s = findBetween(
                            pageContent.substring(fromPosition2, toPosition),
                            "<strong>", " ", 0);

                    if (s == "") {
                        s = findBetween(
                                pageContent.substring(fromPosition2, toPosition),
                                "<strong>", "PLN", 0);
                    }
                    if (s == "") {
                        break;
                    }

                    price = Float.valueOf(s.replace(",", "."));
                }
                if (price == 0.00) {
                    break;
                }

                toPosition = pageContent.indexOf("</article>", fromPosition);
                s = pageContent.substring(fromPosition, toPosition);


                book = new Book();

                book.price = price;

                book.offerExpiryDate = expiryDate;
                book.volumeInfo = new VolumeInfo();
                book.volumeInfo.smallThumbnail = "https://artrage.pl" +
                        findBetween(s, "<img src=\"", "\"", 0);

                book.volumeInfo.title = findBetween(s, "<h4>", "</h4>", 0);

                book.downloadUrl = url;

                book.volumeInfo.authors = new String[1];
                book.volumeInfo.authors[0] =
                        findBetween(s, "<p class=\"author\">", "</p>", 0);

                if (!book.volumeInfo.authors[0].toLowerCase().contains(name.toLowerCase()) &&
                        !book.volumeInfo.title.toLowerCase().contains(name.toLowerCase())) {
                    continue;
                }
                if (addBook(book, books, sortOrder)) {
                    added = true;
                }
                sortOrder++;
            }
            return added;
        }
    }

    public class IBUK extends StoreInfo {
        public String[] getSearchUrl(String name) {
            String[] urls = {"https://www.ibuk.pl/szukaj/ala.html?pid=4&co=" + name +
                    "&od=" + ((pageNumber - 1) * 15) + "&limit=15" +
                    "&typ_publikacji=epub,mobi,pdf"};
            return urls;
        }

        public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books) {
            int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
            String s, s2;
            Book book;
            Boolean added = false;
            while (true) {
                startSearchPosition = toPosition;
                fromPosition = pageContent.indexOf("<div class=\"bookitem\">",
                        startSearchPosition);
                if (fromPosition == -1) {
                    break;
                }
                toPosition = pageContent.indexOf("<div style=\"clear: both; width: 100%;\"></div>", fromPosition);
                s = pageContent.substring(fromPosition, toPosition);

                book = new Book();
                book.offerExpiryDate = null;

                book.volumeInfo = new VolumeInfo();
                book.volumeInfo.smallThumbnail =
                        "https://www.ibuk.pl" +
                                findBetween(
                                        findBetween(s, "<div class=\"tablecell okladaka\">", "</div>", 0),
                                        "<img src=\"", "\"", 0);

                book.volumeInfo.title =
                        findBetween(
                                findBetween(s, "<h2>", "</h2>", 0),
                                "\" >", "<br />", 0);

                book.downloadUrl =
                        "https://www.ibuk.pl" +
                                findBetween(s, "<h2><a href=\"", "\" >", 0);

                book.volumeInfo.authors = new String[1];
                book.volumeInfo.authors[0] =
                        findBetween(
                                findBetween(s, "<span><a href=\"https://www.ibuk.pl/szukaj/", "/a>", 0),
                                "\">", "<", 0);

                s2 = findBetween(s, "Kup teraz za <b style=\"color: black;\"> ",
                        " zł", 0);

                if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                        book.volumeInfo.title.isEmpty() || s2.isEmpty()) {
                    break;
                }

                book.price = Float.valueOf(s2.replace(",", "."));

                if (addBook(book, books, sortOrder)) {
                    added = true;
                }
                sortOrder++;
            }
            return (pageContent.indexOf("<div class=\"pagination font-size14\">") != -1) &&
                    added;
        }
    }

    private class DownloadBooksInfoTask extends AsyncTask<Object, Object, ArrayList<Books>> {
        private final BaseAdapter mAdapter;
        private final Button mSearchButton;
        private final ProgressBar mProgressBar;

        DownloadBooksInfoTask(BaseAdapter adapter, Button searchButton, ProgressBar progressBar) {
            mAdapter = adapter;
            mSearchButton = searchButton;
            mProgressBar = progressBar;

            mProgressBar.setProgress(0);
        }

        private boolean getPageContent(String address, StringBuilder content) {
            if (address.isEmpty()) {
                return false;
            }
            try {
                URL url = new URL(address);
                if (url.getProtocol().equals("https")) {
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setReadTimeout(5000); // 5 seconds
                    connection.connect();
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return false;
                    }
                    try {
                        InputStreamReader isr = new InputStreamReader(connection.getInputStream());
                        BufferedReader in = new BufferedReader(isr);
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                    } finally {
                        connection.disconnect();
                    }

                } else {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(5000); // 5 seconds
                    connection.connect();
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return false;
                    }
                    try {
                        InputStreamReader isr = new InputStreamReader(connection.getInputStream());
                        BufferedReader in = new BufferedReader(isr);
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                    } finally {
                        connection.disconnect();
                    }

                }
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        @Override
        protected ArrayList<Books> doInBackground(Object... params) {
            String nameToSearch = (String) params[0];
            StoreInfo ele = (StoreInfo) params[1];
            StringBuilder content = new StringBuilder();
            ArrayList<Books> allBooks = (ArrayList<Books>) params[2];
            Boolean siteWithContent = false;
            String[] urls = ele.getSearchUrl(nameToSearch);

            for (String singleURL : urls) {
                if (getPageContent(singleURL, content)) {
                    if (ele.doesItMatch(nameToSearch, singleURL, content, allBooks)) {
                        siteWithContent = true;
                    }
                }
                content.setLength(0);
                mProgressBar.setProgress(mProgressBar.getProgress() +
                        ((int) (params[3]) / urls.length));
            }
            if (siteWithContent) {
                sitesWithContent++;
            }
            sitesProcessed++;

            return allBooks;
        }

        protected void onPostExecute(ArrayList<Books> books) {
            if (sitesProcessed == mList.size()) {
                if (sitesWithContent == 0) {
                    pageNumber = -1;
                }

                mProgressBar.setProgress(0);

            /*    Collections.sort(books, new Comparator<Books>() {
                    public int compare(Books obj1, Books obj2) {
                        //  if (obj2.items.get(obj2.positionInMainList).volumeInfo.title.trim().equalsIgnoreCase(mSearchText)) {
                        //    return 1;
                        //}
                        if (obj1.items.get(obj1.positionInMainList).price ==
                                obj2.items.get(obj2.positionInMainList).price) {
                            return obj1.sortOrderInMainList - obj2.sortOrderInMainList;
                        }
                        return (int) (obj1.items.get(obj1.positionInMainList).price * 100 -
                                obj2.items.get(obj2.positionInMainList).price * 100);
                    }
                });
*/
                mSearchButton.setEnabled(true);
                mSearchTextView.setEnabled(true);
                mAdapter.notifyDataSetChanged();
                //notifyDataSetInvalidated();
            }
        }
    }

}
