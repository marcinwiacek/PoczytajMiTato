package com.mwiacek.poczytaj.mi.tato;

import android.content.Context;

import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class FragmentConfig implements Serializable {
    static final long serialVersionUID = 5L; // Version of the structure for serialization

    public HashSet<StoreInfo.StoreInfoTyp> storeInfoForSearchFragment = new HashSet<>();
    public List<String> searchHistory = new ArrayList<>();

    public HashSet<Page.PagesTyp> readInfoForReadFragment = new HashSet<>();
    public boolean showHidden = false;
    public boolean useTOR = false;
    public boolean pobierajTekstyZIndeksem = false;
    public int coIleGodzin = -1;
    public int przyBledzieCoMinut = -1;
    public boolean pobierzPrzyWifi = true;
    public boolean pobierzPrzyGSM = true;
    public boolean pobierzPrzyInnejSieci = true;
    public boolean pobierzTylkoPrzyLadowaniu = false;
    public boolean niePobierajPrzyNiskiejBaterii = true;
    public boolean networkWithoutLimit = false;

    public int tabNum;
    public String tabName;

    FragmentConfig(int tabNum, String tabName) {
        this.tabNum = tabNum;
        this.tabName = tabName;
    }

    static FragmentConfig readFromInternalStorage(Context context, int tabNum) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(context.openFileInput("tab" + tabNum));
            FragmentConfig t = (FragmentConfig) inputStream.readObject();
            inputStream.close();
            return t;
        } catch (Exception ignoreException) {
            return null;
        }
    }

    public void saveToInternalStorage(Context context) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(
                    context.openFileOutput("tab" + tabNum, Context.MODE_PRIVATE));
            outputStream.writeObject(this);
            outputStream.close();
        } catch (Exception ignored) {
        }
    }

    private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.defaultWriteObject();
    }
}
