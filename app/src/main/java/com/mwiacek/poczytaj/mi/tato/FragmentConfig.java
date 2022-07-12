package com.mwiacek.poczytaj.mi.tato;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/* Configuration for ReadFragment and SearchFragment */
public class FragmentConfig implements Serializable, Cloneable {
    static final long serialVersionUID = 9L; // Version of the structure for serialization

    /* Search Fragment */
    public HashSet<StoreInfo.StoreInfoTyp> storeInfoForSearchFragment = new HashSet<>();
    public List<String> searchHistory = new ArrayList<>();

    /* Read Fragment */
    public HashSet<Page.PageTyp> readInfoForReadFragment = new HashSet<>();
    public boolean showHiddenTexts = false;
    public boolean useTOR = false;
    public boolean getTextsWhenRefreshingIndex = false;
    public boolean downloadWithWifi = true;
    public boolean downloadWithGSM = true;
    public boolean downloadWithOtherNet = true;
    public boolean downloadDuringChargingOnly = false;
    public boolean doNotDownloadWithLowBattery = true;
    public boolean networkWithoutLimit = false;
    public int howOftenRefreshTabInHours = -1;
    public int howOftenTryToRefreshTabAfterErrorInMinutes = -1;

    /* tab related info */
    public int tabNumForFileForSerialization;
    public String tabName;

    FragmentConfig(int tabNum, String tabName) {
        this.tabNumForFileForSerialization = tabNum;
        this.tabName = tabName;
    }

    static FragmentConfig readFromInternalStorage(Context context, int tabNum) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(
                    context.openFileInput("tab" + tabNum));
            FragmentConfig t = (FragmentConfig) inputStream.readObject();
            inputStream.close();
            return t;
        } catch (Exception ignore) {
            return null;
        }
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void saveToInternalStorage(Context context) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(
                    context.openFileOutput("tab" + tabNumForFileForSerialization,
                            Context.MODE_PRIVATE));
            outputStream.writeObject(this);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.defaultWriteObject();
    }
}
