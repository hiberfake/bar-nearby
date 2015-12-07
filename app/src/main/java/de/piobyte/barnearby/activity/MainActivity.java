package de.piobyte.barnearby.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.client.Firebase;
import com.firebase.ui.FirebaseRecyclerViewAdapter;
import com.github.florent37.glidepalette.GlidePalette;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;

import de.piobyte.barnearby.R;
import de.piobyte.barnearby.data.Locality;
import de.piobyte.barnearby.util.UiUtils;
import de.piobyte.barnearby.widget.OffsetDecoration;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private GoogleApiClient mGoogleApiClient;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private CoordinatorLayout mCoordinatorLayout;
    private FloatingActionButton mFab;
    private ContentLoadingProgressBar mProgressBar;

    private final View.OnClickListener mFabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            subscribe();
        }
    };

    private final Runnable mPermissionStatusRunnable = new Runnable() {
        @Override
        public void run() {
            // Subscribe to receive nearby messages created by this Developer Console project.
            // Use Strategy.BLE_ONLY because we are only interested in messages
            // that we attached to BLE beacons.
            Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, Strategy.BLE_ONLY)
                    .setResultCallback(new CheckingCallback("subscribe()", mSubscribeRunnable));
        }
    };

    private final Runnable mSubscribeRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCoordinatorLayout == null) {
                return;
            }
            Snackbar.make(mCoordinatorLayout, R.string.nearby_running, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.nearby_stop, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            unsubscribe();
                        }
                    })
                    .show();
        }
    };

    private final MessageListener mMessageListener = new MessageListener() {
        // Called each time a new message is discovered nearby.
        @Override
        public void onFound(Message message) {
            Log.i(TAG, "Found message: " + message);
            Log.i(TAG, "Message content: " + new String(message.getContent()));
        }

        // Called when a message is no longer nearby.
        @Override
        public void onLost(Message message) {
            Log.i(TAG, "Lost message: " + message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        String uuid = "E20A39F473F54BC4A12F17D1AD07A961";
//        String major = "0000";
//        String minor = "0000";
//        byte[] input = hexStringToByteArray(uuid + major + minor);
//        Log.wtf(TAG, Base64.encodeToString(input, Base64.DEFAULT));

//        getWindow().setBackgroundDrawableResource(R.color.grey_200);

        final int spanCount = getResources().getInteger(R.integer.span_count);
        final int spacing = getResources().getDimensionPixelSize(R.dimen.spacing);

        float imageWidth = UiUtils.getScreenWidth(this);
        imageWidth -= (spanCount + 1) * spacing;
        imageWidth /= spanCount;
        final int imageHeight = (int) (imageWidth / (4f / 3f));

        Firebase firebase = new Firebase("https://scorching-torch-4683.firebaseio.com/localities");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        setupToolbar();

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(mFabClickListener);
        mProgressBar = (ContentLoadingProgressBar) findViewById(R.id.progress_bar);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerView.addItemDecoration(new OffsetDecoration(spacing));

        FirebaseRecyclerViewAdapter<Locality, ViewHolder> adapter =
                new FirebaseRecyclerViewAdapter<Locality, ViewHolder>(Locality.class,
                        R.layout.grid_item, ViewHolder.class, firebase) {
                    @Override
                    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        ViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
                        viewHolder.imageView.getLayoutParams().height = imageHeight;
                        return viewHolder;
                    }

                    @Override
                    protected void populateViewHolder(ViewHolder viewHolder, Locality locality) {
                        if (mProgressBar.isShown()) {
                            mProgressBar.hide();
                        }
                        populateImage(viewHolder, locality.getImage());
//                        viewHolder.textBackgroundView.setVisibility(View.INVISIBLE);
                        viewHolder.textView.setText(locality.getName());
                    }
                };
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (messages != null) {
                NdefMessage[] ndefMessages = new NdefMessage[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    ndefMessages[i] = (NdefMessage) messages[i];
                }

                for (NdefMessage ndefMessage : ndefMessages) {
                    NdefRecord[] ndefRecords = ndefMessage.getRecords();
                    for (NdefRecord ndefRecord : ndefRecords) {
                        Log.i(TAG, "NDEF payload: " + new String(ndefRecord.getPayload()));
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            unsubscribe();
        }
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect.
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                } else {
                    // Permission granted or error resolved successfully
                    // then we proceed with subscribe.
                    subscribe();
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Show the FAB.
        mFab.show();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Hide the FAB.
        mFab.hide();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mResolvingError) {
            if (result.hasResolution()) {
                try {
                    mResolvingError = true;
                    result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    mGoogleApiClient.connect();
                }
            } else {
                // Show dialog using GoogleApiAvailability.getErrorDialog()
                showErrorDialog(result.getErrorCode());
                mResolvingError = true;
            }
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    private void populateImage(final ViewHolder viewHolder, String image) {
//        Glide.with(this)
//                .load(image)
//                .asBitmap()
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .centerCrop()
//                .into(new BitmapImageViewTarget(viewHolder.imageView) {
//                    @Override
//                    public void onResourceReady(final Bitmap bitmap,
//                                                final GlideAnimation<? super Bitmap> animation) {
////                        super.onResourceReady(bitmap, animation);
//                        onBitmapReady(viewHolder, bitmap);
//                    }
//                });
        Glide.with(this)
                .load(image)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .centerCrop()
                .crossFade()
                .listener(GlidePalette.with(image)
                        .use(GlidePalette.Profile.VIBRANT)
                        .intoBackground(viewHolder.itemView.findViewById(R.id.vibrant))
                        .use(GlidePalette.Profile.VIBRANT_DARK)
                        .intoBackground(viewHolder.itemView.findViewById(R.id.vibrant_dark))
                        .use(GlidePalette.Profile.VIBRANT_LIGHT)
                        .intoBackground(viewHolder.itemView.findViewById(R.id.vibrant_light))
                        .use(GlidePalette.Profile.MUTED)
                        .intoBackground(viewHolder.itemView.findViewById(R.id.muted))
                        .use(GlidePalette.Profile.MUTED_DARK)
                        .intoBackground(viewHolder.itemView.findViewById(R.id.muted_dark))
                        .use(GlidePalette.Profile.MUTED_LIGHT)
                        .intoBackground(viewHolder.itemView.findViewById(R.id.muted_light)))
                .into(viewHolder.imageView);
    }

    private void onBitmapReady(ViewHolder viewHolder, final Bitmap bitmap) {
        final ImageView imageView = viewHolder.imageView;
        final View textBackgroundView = viewHolder.textBackgroundView;
        Palette.PaletteAsyncListener listener = new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                imageView.setImageBitmap(bitmap);
                int color = palette.getDarkVibrantColor(Color.TRANSPARENT);
                if (Color.TRANSPARENT == color) {
                    color = palette.getDarkMutedColor(Color.TRANSPARENT);
                }
                textBackgroundView.setBackgroundColor(color);
                textBackgroundView.setVisibility(View.VISIBLE);

                final Animator imageAnimation, textAnimation;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imageAnimation = ViewAnimationUtils.createCircularReveal(
                            imageView,
                            imageView.getWidth() / 2, imageView.getHeight(),
                            0, imageView.getWidth());
                    textAnimation = ViewAnimationUtils.createCircularReveal(
                            textBackgroundView,
                            textBackgroundView.getWidth() / 2, 0,
                            0, textBackgroundView.getWidth());
                } else {
                    imageView.setAlpha(0f);
                    imageView.animate().alpha(1f);
                    imageAnimation = ObjectAnimator.ofFloat(imageView, "alpha", 1f);
                    textBackgroundView.setAlpha(0f);
                    textBackgroundView.animate().alpha(1f);
                    textAnimation = ObjectAnimator.ofFloat(textBackgroundView, "alpha", 1f);
                }
                AnimatorSet animation = new AnimatorSet();
                animation.playTogether(imageAnimation, textAnimation);
                animation.setInterpolator(INTERPOLATOR);
                animation.start();
            }
        };
        new Palette.Builder(bitmap).generate(listener);
    }

    private void subscribe() {
        // Check the permission and in case of success do the subscription.
        Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(
                new CheckingCallback("getPermissionStatus()", mPermissionStatusRunnable));
    }

    private void unsubscribe() {
        // Clean up when the user leaves the activity.
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
                .setResultCallback(new CheckingCallback("unsubscribe()"));
    }

    /* Creates a dialog for an error message. */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), DIALOG_ERROR);
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    private void onDialogDismissed() {
        mResolvingError = false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final View textBackgroundView;
        private final TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            textBackgroundView = itemView.findViewById(R.id.text_background);
            textView = (TextView) itemView.findViewById(R.id.text);
        }
    }

    /* A fragment to display an error dialog. */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance()
                    .getErrorDialog(getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }

    /**
     * A simple ResultCallback that displays a toast when errors occur.
     * It also displays the Nearby opt-in dialog when necessary.
     */
    private class CheckingCallback implements ResultCallback<Status> {
        private final String method;
        private final Runnable runOnSuccess;

        private CheckingCallback(String method) {
            this(method, null);
        }

        private CheckingCallback(String method, @Nullable Runnable runOnSuccess) {
            this.method = method;
            this.runOnSuccess = runOnSuccess;
        }

        @Override
        public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
                Log.i(TAG, method + " succeeded.");
                if (runOnSuccess != null) {
                    runOnSuccess.run();
                }
            } else {
                // Currently, the only resolvable error is that the device is not opted
                // in to Nearby. Starting the resolution displays an opt-in dialog.
                if (status.hasResolution()) {
                    if (!mResolvingError) {
                        try {
                            status.startResolutionForResult(MainActivity.this,
                                    REQUEST_RESOLVE_ERROR);
                            mResolvingError = true;
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, method + " failed with exception: ", e);
                        }
                    } else {
                        // This will be encountered on initial startup because we do
                        // both publish and subscribe together. So having a toast while
                        // resolving dialog is in progress is confusing, so just log it.
                        Log.i(TAG, method + " failed with status: " + status
                                + " while resolving error.");
                    }
                } else {
                    Log.e(TAG, method + " failed with : " + status
                            + " resolving error: " + mResolvingError);
                }
            }
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}