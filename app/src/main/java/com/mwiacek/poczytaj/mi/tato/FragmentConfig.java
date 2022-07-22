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
import java.util.List;

/* Configuration for ReadFragment and SearchFragment */
public class FragmentConfig implements Serializable, Cloneable {
    static final long serialVersionUID = 21L; // Version of the structure for serialization
    /* Search Fragment */
    public ArrayList<StoreInfo.StoreInfoTyp> storeInfoForSearchFragment = new ArrayList<>();
    /* Read Fragment */
    public ArrayList<Page.PageTyp> readInfoForReadFragment = new ArrayList<>();
    public HiddenTexts showHiddenTexts = HiddenTexts.NONE;
    public boolean useTOR = false;
    public boolean getTextsWhenRefreshingIndex = false;
    public boolean canDownloadWithWifi = true;
    public boolean canDownloadWithGSM = true;
    public boolean canDownloadInNetworkWithLimit = false;
    public boolean canDownloadOnRoaming = false;
    public boolean canDownloadWithOtherNetwork = true;
    public boolean canDownloadWithoutCharging = true;
    public boolean canDownloadWithLowBattery = false;
    public boolean canDownloadWithLowStorage = false;
    public String authorFilter = "";
    public String tagFilter = "";
    public int howOftenRefreshTabInHours = -1;
    public int howOftenTryToRefreshTabAfterErrorInMinutes = -1;
    /* tab related info */
    public int fileNameTabNum;
    public String tabName;
    boolean searchFragmentConfig = false;

    FragmentConfig(int tabNum, String tabName) {
        this.fileNameTabNum = tabNum;
        this.tabName = tabName;
    }

    public static FragmentConfig readFromInternalStorage(Context context, int tabNum) {
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
                    context.openFileOutput("tab" + fileNameTabNum,
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

    public enum HiddenTexts {
        NONE,
        GREEN,
        RED
    }
}
