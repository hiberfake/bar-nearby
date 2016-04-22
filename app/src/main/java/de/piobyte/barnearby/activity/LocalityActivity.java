package de.piobyte.barnearby.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.piobyte.barnearby.BuildConfig;
import de.piobyte.barnearby.R;
import de.piobyte.barnearby.data.Locality;
import de.piobyte.barnearby.util.SystemUtils;

public class LocalityActivity extends AppCompatActivity {

    static final String EXTRA_LOCALITY = BuildConfig.APPLICATION_ID + ".EXTRA.locality";

    private Toolbar mToolbar;
    private ImageView mImageView;

    private boolean mIsReturning;
    private boolean mIsCollapsing;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            Log.i("Locality", "Map shared elements");
            if (mIsReturning && mIsCollapsing) {
                String transitionName = ViewCompat.getTransitionName(mImageView);
                names.remove(transitionName);
                sharedElements.remove(transitionName);
            }
        }
    };
    private final AppBarLayout.OnOffsetChangedListener mOffsetChangedListener
            = new AppBarLayout.OnOffsetChangedListener() {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            mIsCollapsing = Math.abs(verticalOffset) * 2 > appBarLayout.getHeight();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locality);

        final Locality locality = (Locality) getIntent().getSerializableExtra(EXTRA_LOCALITY);

        Firebase firebase = new Firebase("https://scorching-torch-4683.firebaseio.com/menus");
        firebase.equalTo(locality.getMenu());
        firebase.child(locality.getMenu()).child("drinks")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        TreeMap drinks = dataSnapshot.getValue(TreeMap.class);
                        if (drinks == null) {
                            return;
                        }

                        ((TextView) findViewById(R.id.drink_one_name))
                                .setText(drinks.firstKey().toString());
                        ((TextView) findViewById(R.id.drink_one_price))
                                .setText(drinks.firstEntry().getValue().toString() + " €");

                        ((TextView) findViewById(R.id.drink_two_name))
                                .setText(drinks.lastKey().toString());
                        ((TextView) findViewById(R.id.drink_two_price))
                                .setText(drinks.lastEntry().getValue().toString() + " €");
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                    }
                });
        firebase.child(locality.getMenu()).child("meals")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        TreeMap meals = dataSnapshot.getValue(TreeMap.class);
                        if (meals == null) {
                            return;
                        }

                        ((TextView) findViewById(R.id.meal_one_name))
                                .setText(meals.firstKey().toString());
                        ((TextView) findViewById(R.id.meal_one_price))
                                .setText(meals.firstEntry().getValue().toString() + " €");

                        ((TextView) findViewById(R.id.meal_two_name))
                                .setText(meals.lastKey().toString());
                        ((TextView) findViewById(R.id.meal_two_price))
                                .setText(meals.lastEntry().getValue().toString() + " €");
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                    }
                });

        setupToolbar();

        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener(mOffsetChangedListener);
        }
        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(locality.getName());
        }

        mImageView = (ImageView) findViewById(R.id.image);
        ViewCompat.setTransitionName(mImageView, locality.getImage());

        Glide.with(this)
                .load(locality.getImage())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .priority(Priority.IMMEDIATE)
                .centerCrop()
                .dontAnimate()
                .into(mImageView);

        mImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver
                .OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                ActivityCompat.startPostponedEnterTransition(LocalityActivity.this);
                return true;
            }
        });

        ActivityCompat.postponeEnterTransition(this);
        ActivityCompat.setEnterSharedElementCallback(this, mCallback);

        if (SystemUtils.isAtLeastLollipop()) {
            Transition enterTransition = getWindow().getSharedElementEnterTransition();
            enterTransition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    Log.i("Enter", "Transition start");
//                    animateStatusBar(Color.TRANSPARENT);
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    Log.i("Enter", "Transition end");
//                    toggleToolbar();
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                }

                @Override
                public void onTransitionPause(Transition transition) {
                }

                @Override
                public void onTransitionResume(Transition transition) {
                }
            });

            Transition returnTransition = getWindow().getSharedElementReturnTransition();
            returnTransition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    Log.i("Return", "Transition start");
//                    animateStatusBar(
//                            ContextCompat.getColor(LocalityActivity.this, R.color.primary_dark));
//                    toggleToolbar();
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    Log.i("Return", "Transition end");
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                }

                @Override
                public void onTransitionPause(Transition transition) {
                }

                @Override
                public void onTransitionResume(Transition transition) {
                }
            });
        }
    }

    @Override
    public void supportFinishAfterTransition() {
        mIsReturning = true;
        super.supportFinishAfterTransition();
    }

    private void setupToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
//            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
//            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    supportFinishAfterTransition();
//                }
//            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateStatusBar(int color) {
        ValueAnimator animation = ValueAnimator.ofArgb(getWindow().getStatusBarColor(), color);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                getWindow().setStatusBarColor((int) animation.getAnimatedValue());
            }
        });
//        animation.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        animation.start();
    }

    private void toggleToolbar() {
        if (mToolbar.isShown()) {
            mToolbar.animate()
                    .alpha(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mToolbar.setVisibility(View.INVISIBLE);
                        }
                    });
        } else {
            mToolbar.setVisibility(View.VISIBLE);
            mToolbar.setAlpha(0f);
            mToolbar.animate()
                    .alpha(1f)
                    .setDuration(5000)
                    .setListener(null);
        }
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }
}