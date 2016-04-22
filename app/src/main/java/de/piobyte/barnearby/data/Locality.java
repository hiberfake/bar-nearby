package de.piobyte.barnearby.data;

import java.io.Serializable;

public class Locality implements Serializable {

    private String image;
    private String name;
    private String menu;

    public String getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getMenu() {
        return menu;
    }

    //    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel out, int flags) {
//        out.writeString(image);
//        out.writeString(name);
//    }
//
//    public static final Parcelable.Creator<Locality> CREATOR
//            = new Parcelable.Creator<Locality>() {
//        public Locality createFromParcel(Parcel in) {
//            return new Locality(in);
//        }
//
//        public Locality[] newArray(int size) {
//            return new Locality[size];
//        }
//    };
//
//    private Locality(Parcel in) {
//        image = in.readString();
//        name = in.readString();
//    }
}