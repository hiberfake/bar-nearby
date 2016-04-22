package de.piobyte.barnearby.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NdefRecord;
import android.text.TextUtils;

import com.google.android.gms.nearby.messages.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CacheUtils {

    private static final String KEY_CACHED_NDEF_RECORD = "cached-ndef-record";
    private static final String KEY_CACHED_MESSAGES = "cached-messages";


    /**
     * Fetches the Ndef record stored in {@link SharedPreferences}.
     *
     * @param context The context.
     * @return The Ndef record (possibly null).
     */
    public static NdefRecord getCachedNdefRecord(Context context) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        String cachedNdefRecord = sharedPrefs.getString(KEY_CACHED_NDEF_RECORD, "");
        if (TextUtils.isEmpty(cachedNdefRecord)) {
            return null;
        } else {
            return new Gson().fromJson(cachedNdefRecord, NdefRecord.class);
        }
    }

    /**
     * Remove the Ndef record stored in {@link SharedPreferences}.
     */
    public static void clearCachedNdefRecord(Context context) {
        getSharedPreferences(context)
                .edit()
                .remove(KEY_CACHED_NDEF_RECORD)
                .apply();
    }

    /**
     * Saves the Ndef record to {@link SharedPreferences}.
     *
     * @param context    The context.
     * @param ndefRecord The Ndef record saved to SharedPreferences.
     */
    public static void saveNdefRecord(Context context, NdefRecord ndefRecord) {
        getSharedPreferences(context)
                .edit()
                .putString(KEY_CACHED_NDEF_RECORD, new Gson().toJson(ndefRecord, NdefRecord.class))
                .apply();
    }

    /**
     * Fetches message strings stored in {@link SharedPreferences}.
     *
     * @param context The context.
     * @return A list (possibly empty) containing message strings.
     */
    public static List<Message> getCachedMessages(Context context) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        String cachedMessagesJson = sharedPrefs.getString(KEY_CACHED_MESSAGES, "");
        if (TextUtils.isEmpty(cachedMessagesJson)) {
            return Collections.emptyList();
        } else {
            Type type = new TypeToken<List<Message>>() {
            }.getType();
            return new Gson().fromJson(cachedMessagesJson, type);
        }
    }

    /**
     * Removes all messages stored in {@link SharedPreferences}.
     */
    public static void clearCachedMessages(Context context) {
        getSharedPreferences(context)
                .edit()
                .remove(KEY_CACHED_MESSAGES)
                .apply();
    }

    /**
     * Saves a message string to {@link SharedPreferences}.
     *
     * @param context The context.
     * @param message The Message whose payload (as string) is saved to SharedPreferences.
     */
    public static void saveFoundMessage(Context context, Message message) {
        ArrayList<Message> cachedMessages = new ArrayList<>(getCachedMessages(context));
        cachedMessages.add(0, message);
        getSharedPreferences(context)
                .edit()
                .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                .apply();
    }

    /**
     * Removes a message string from {@link SharedPreferences}.
     *
     * @param context The context.
     * @param message The Message whose payload (as string) is removed from SharedPreferences.
     */
    public static void removeLostMessage(Context context, Message message) {
        ArrayList<Message> cachedMessages = new ArrayList<>(getCachedMessages(context));
        cachedMessages.remove(message);
        getSharedPreferences(context)
                .edit()
                .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                .apply();
    }

    /**
     * Gets the SharedPReferences object that is used for persisting data in this application.
     *
     * @param context The context.
     * @return The single {@link SharedPreferences} instance that can be used to retrieve and modify
     * values.
     */
    static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(
                context.getApplicationContext().getPackageName(),
                Context.MODE_PRIVATE);
    }
}