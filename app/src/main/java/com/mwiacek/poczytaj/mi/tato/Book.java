package com.mwiacek.poczytaj.mi.tato;

import java.util.Date;

class Book {
    public float price;
    public String downloadUrl;
    public VolumeInfo volumeInfo;
    public Date offerExpiryDate;
}

class VolumeInfo {
    public String[] authors;
    // --Commented out by Inspection (05-Nov-17 20:29):public String description;
    public String smallThumbnail;
    // --Commented out by Inspection (05-Nov-17 20:29):public String publisher;
    // --Commented out by Inspection (05-Nov-17 20:29):public String publishedDate;
    public String title;
}
