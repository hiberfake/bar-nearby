package de.piobyte.barnearby.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.ArrayList;

import de.piobyte.barnearby.R;
import de.piobyte.barnearby.adapter.LocalitiesAdapter;
import de.piobyte.barnearby.service.BackgroundSubscribeIntentService;
import de.piobyte.barnearby.util.CacheUtils;
import de.piobyte.barnearby.widget.OffsetDecoration;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocalitiesAdapter.OnItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private static final String KEY_SUB_STATE = "sub-state";
    private static final String KEY_RESOLVING_ERROR = "resolving-error";

    private static final long DEFAULT_DELAY = 1200;

    // Enum to track subscription state.
    private enum SubState {
        NOT_SUBSCRIBING,
        ATTEMPTING_TO_SUBSCRIBE,
        SUBSCRIBING,
        ATTEMPTING_TO_UNSUBSCRIBE
    }

    private Handler mHandler;

    /**
     * Entry point for Google Play services.
     */
    private GoogleApiClient mGoogleApiClient;

    // Fields for tracking subscription state.
    private SubState mSubState = SubState.NOT_SUBSCRIBING;

    /**
     * Tracks if we are currently resolving an error related to Nearby permissions. Used to avoid
     * duplicate Nearby permission dialogs if the user initiates both subscription and publication
     * actions without having opted into Nearby.
     */
    private boolean mResolvingError = false;

    private CoordinatorLayout mCoordinatorLayout;
    private FloatingActionButton mFab;
    private ContentLoadingProgressBar mProgressBar;
    private RecyclerView mRecyclerView;

    private LocalitiesAdapter mAdapter;

    // Create a new message listener.
    private MessageListener mMessageListener = new MessageListener() {
        @Override
        public void onFound(final Message message) {
            int position = Integer.parseInt(new String(message.getContent()));
            // Do something with the message string.
            Log.i(TAG, "Found: " + position);

            View child = mRecyclerView.getLayoutManager().getChildAt(position - 1);
            onItemClick(position - 1, child.findViewById(R.id.image));
        }

        // Called when a message is no longer detectable nearby.
        public void onLost(final Message message) {
            String nearbyMessageString = new String(message.getContent());
            // Take appropriate action here (update UI, etc.)
            Log.i(TAG, "Lost: " + nearbyMessageString);
        }
    };

    private View.OnClickListener mFabClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (CacheUtils.getCachedNdefRecord(MainActivity.this) == null) {
                showScanNecessary();
                return;
            }

            switch (mSubState) {
                case NOT_SUBSCRIBING:
                case ATTEMPTING_TO_UNSUBSCRIBE:
                    mSubState = SubState.ATTEMPTING_TO_SUBSCRIBE;
                    subscribe();
                    break;
                case SUBSCRIBING:
                case ATTEMPTING_TO_SUBSCRIBE:
                    mSubState = SubState.ATTEMPTING_TO_UNSUBSCRIBE;
                    unsubscribe();
                    break;
            }
            updateFab();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mSubState = (SubState) savedInstanceState.getSerializable(KEY_SUB_STATE);
            mResolvingError = savedInstanceState.getBoolean(KEY_RESOLVING_ERROR);
        }

        int spacing = getResources().getDimensionPixelSize(R.dimen.spacing);

        mHandler = new Handler();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();

        setupToolbar();

        Firebase firebase = new Firebase("https://scorching-torch-4683.firebaseio.com/localities");

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(mFabClickListener);
        }
        mProgressBar = (ContentLoadingProgressBar) findViewById(R.id.progress_bar);

        mAdapter = new LocalitiesAdapter(firebase, this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        if (mRecyclerView != null) {
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.addItemDecoration(new OffsetDecoration(spacing));
            mRecyclerView.addOnChildAttachStateChangeListener(
                    new RecyclerView.OnChildAttachStateChangeListener() {
                        @Override
                        public void onChildViewAttachedToWindow(View view) {
                            if (mProgressBar != null) {
                                mProgressBar.hide();
                            }
                            mFab.show();
                            mRecyclerView.removeOnChildAttachStateChangeListener(this);
                        }

                        @Override
                        public void onChildViewDetachedFromWindow(View view) {
                        }
                    });
            mRecyclerView.setAdapter(mAdapter);
        }

        updateFab();

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent");
        super.onNewIntent(intent);
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
                        // Cache the payload and subscribe for Nearby messages.
                        CacheUtils.saveNdefRecord(this, ndefRecord);

                        mSubState = SubState.ATTEMPTING_TO_UNSUBSCRIBE;
                        unsubscribe();

                        showTable();

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mSubState = SubState.ATTEMPTING_TO_SUBSCRIBE;
                                subscribe();
                                updateFab();
                            }
                        }, DEFAULT_DELAY);
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();

        if (isFinishing() && !isChangingConfigurations()) {
            mSubState = SubState.ATTEMPTING_TO_UNSUBSCRIBE;
            unsubscribe();
            BackgroundSubscribeIntentService.cancelNotification(this);
            CacheUtils.clearCachedNdefRecord(this);
            CacheUtils.clearCachedMessages(this);
        }
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_SUB_STATE, mSubState);
        outState.putBoolean(KEY_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mAdapter.cleanup();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // User was presented with the Nearby opt-in dialog and pressed "Allow".
                // Execute the pending subscription and publication tasks here.
                executePendingSubscriptionTask();
                // Make sure the app is not already connected or attempting to connect.
//                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
//                    mGoogleApiClient.connect();
//                } else {
//                    // Permission granted or error resolved successfully
//                    // then we proceed with subscribe.
//                    subscribe();
//                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User declined to opt-in. Reset application state here.
                resetToDefaultState();
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        executePendingSubscriptionTask();
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle the failure silently.
    }

    @Override
    public void onItemClick(int position, View sharedElement) {
        Intent intent = new Intent(MainActivity.this, LocalityActivity.class);
        intent.putExtra(LocalityActivity.EXTRA_LOCALITY, mAdapter.getItem(position));

        ArrayList<Pair<View, String>> pairs = new ArrayList<>();

        pairs.add(Pair.create(sharedElement, ViewCompat.getTransitionName(sharedElement)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View statusBar = findViewById(android.R.id.statusBarBackground);
            View navigationBar = findViewById(android.R.id.navigationBarBackground);

            pairs.add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
            pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
        }

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, pairs.toArray(new Pair[pairs.size()]));
        ActivityCompat.startActivity(MainActivity.this, intent, options.toBundle());
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    private void subscribe() {
        // Check the permission and in case of success do the subscription.
//        Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(
//                new CheckingCallback("getPermissionStatus()", mPermissionStatusRunnable));
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
            return;
        }

//        // Clean start every time we start subscribing.
//        CacheUtils.clearCachedMessage(this);

        Log.i(TAG, "Subscribing for background updates");
        SubscribeOptions options = new SubscribeOptions.Builder()
                // Finds messages attached to BLE beacons.
                // See https://developers.google.com/beacons/
                .setStrategy(Strategy.BLE_ONLY)
                .build();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Subscribed successfully");
                            // Subscribed successfully.
                            mSubState = SubState.SUBSCRIBING;
                            // Start background service.
//                            startService(getBackgroundSubscribeServiceIntent());
                            BackgroundSubscribeIntentService.updateNotification(MainActivity.this);
                        } else {
                            // Could not subscribe.
                            handleUnsuccessfulNearbyResult(status);
                        }
                    }
                });
    }

    private void unsubscribe() {
        // Clean up when the user leaves the activity.
//        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener).setResultCallback(
//                new CheckingCallback("unsubscribe()", mUnsubscribeRunnable));
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
            return;
        }

        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Unsubscribed successfully");
                            // Unsubscribed successfully.
                            mSubState = SubState.NOT_SUBSCRIBING;
                            BackgroundSubscribeIntentService.cancelNotification(MainActivity.this);
                        } else {
                            // Could not unsubscribe.
                            handleUnsuccessfulNearbyResult(status);
                        }
                    }
                });
    }

//    private PendingIntent getPendingIntent() {
//        return PendingIntent.getService(this, 0, getBackgroundSubscribeServiceIntent(),
//                PendingIntent.FLAG_UPDATE_CURRENT);
//    }

//    private Intent getBackgroundSubscribeServiceIntent() {
//        return new Intent(this, BackgroundSubscribeIntentService.class);
//    }

    private void handleUnsuccessfulNearbyResult(Status status) {
        if (status.hasResolution()) {
            if (!mResolvingError) {
                try {
                    mResolvingError = true;
                    status.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    mResolvingError = false;
                    Log.i(TAG, "Failed to resolve error status.", e);
                }
            }
        } else if (!status.isSuccess()) {
            if (status.getStatusCode() == CommonStatusCodes.NETWORK_ERROR) {
                Toast.makeText(this, "Keine Internetverbindung", Toast.LENGTH_LONG).show();
            } else {
                // To keep things simple, pop a toast for all other error messages.
                Toast.makeText(this, status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
            resetToDefaultState();
        }
    }

    /**
     * Invokes a pending task based on the subscription and publication states.
     */
    private void executePendingSubscriptionTask() {
        if (mSubState == SubState.ATTEMPTING_TO_SUBSCRIBE) {
            subscribe();
        } else if (mSubState == SubState.ATTEMPTING_TO_UNSUBSCRIBE) {
            unsubscribe();
        }
    }

    private void updateFab() {
        switch (mSubState) {
            case ATTEMPTING_TO_SUBSCRIBE:
            case SUBSCRIBING:
                mFab.setImageResource(R.drawable.ic_stop_white_24dp);
                break;
            default:
                mFab.setImageResource(R.drawable.ic_nearby_white_24dp);
        }
    }

    private void showScanNecessary() {
        Snackbar.make(mCoordinatorLayout, R.string.scan_necessary, Snackbar.LENGTH_SHORT)
                .show();
    }

    private void showTable() {
        NdefRecord ndefRecord = CacheUtils.getCachedNdefRecord(this);
        if (ndefRecord != null) {
            String payload = new String(ndefRecord.getPayload());
            String text = getString(R.string.choosen_table, payload);
            Snackbar.make(mCoordinatorLayout, text, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Resets the state of pending subscription and publication tasks.
     */
    private void resetToDefaultState() {
        mSubState = SubState.NOT_SUBSCRIBING;
        updateFab();
    }
}