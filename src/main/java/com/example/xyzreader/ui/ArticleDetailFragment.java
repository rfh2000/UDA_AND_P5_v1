package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private View mPhotoContainerView;
    private ImageView mPhotoView;
    //private NetworkImageView mPhotoView;
    //private DynamicHeightNetworkImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // CHANGE THE FRAGMENT DETAIL LAYOUT HERE
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        if (!isLandscape()) {
            mPhotoView.getLayoutParams().height = (int) findScreenAxisLength("height") / 3 * 2;
            Log.v("ONCREATEVIEW", "New height is " + (int) findScreenAxisLength("height")/3*2);
            mPhotoView.requestLayout();
        }
        //Log.v("ONCREATEVIEW", "New height is " + (int) findScreenAxisLength("height")/3*2);
        //mPhotoView = (NetworkImageView) mRootView.findViewById(R.id.photo);
        //mPhotoView = (DynamicHeightNetworkImageView) mRootView.findViewById(R.id.photo);

        //mPhotoContainerView = mRootView.findViewById(R.id.photo_container);

        mStatusBarColorDrawable = new ColorDrawable(0);

        bindViews();
//        updateStatusBar();

        findScreenSize();

        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final TextView tvTitle = (TextView) view.findViewById(R.id.fragment_article_title);
        final TextView tvBody = (TextView) view.findViewById(R.id.fragment_article_body);
//        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
        view.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                TextView tvTitle = (TextView) view.findViewById(R.id.fragment_article_title);
//                TextView tvBody = (TextView) view.findViewById(R.id.fragment_article_body);
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setSubject(tvTitle.getText().toString())
                        .setText(tvBody.getText().toString())
                        .getIntent(), getString(R.string.action_share)));
            }
        });
    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
//        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.fragment_article_title);
        TextView subTitleView = (TextView) mRootView.findViewById(R.id.fragment_article_subtitle);
//        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
//        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.fragment_article_body);
        //bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

//            mCollapsingToolbarLayout.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));

            subTitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));

//            bylineView.setText(Html.fromHtml(
//                    DateUtils.getRelativeTimeSpanString(
//                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
//                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
//                            DateUtils.FORMAT_ABBREV_ALL).toString()
//                            + " by <font color='#ffffff'>"
//                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
//                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));



            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();

                            if (bitmap != null) {
//                                Palette p = Palette.generate(bitmap, 12);
//                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());

//                                mRootView.findViewById(R.id.meta_bar)
//                                        .setBackgroundColor(mMutedColor);
//                                mRootView.findViewById(R.id.meta_bar)
//                                        .setBackgroundColor(getResources().getColor(R.color.theme_primary));
//                                mRootView.findViewById(R.id.meta_bar)
//                                        .setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//                                updateStatusBar();
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            subTitleView.setText("N/A");
            //bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }


    public void findScreenSize(){
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        Log.v("FINDSCREENSIZE", "Pixel width is " + widthPixels);
        int heightPixels = metrics.heightPixels;
        Log.v("FINDSCREENSIZE", "Pixel height is " + heightPixels);
        float scaleFactor = metrics.density;
        Log.v("FINDSCREENSIZE", "Scale factor is " + scaleFactor);
        float widthDp = widthPixels / scaleFactor;
        Log.v("FINDSCREENSIZE", "Pixel width dp is " + widthDp);
        float heightDp = heightPixels / scaleFactor;
        Log.v("FINDSCREENSIZE", "Pixel height dp is " + heightDp);
        float smallestWidth = Math.min(widthDp, heightDp);
    }

    public float findScreenAxisLength(String axis){
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        Log.v("FINDSCREENSIZE", "Pixel width is " + widthPixels);
        int heightPixels = metrics.heightPixels;
        Log.v("FINDSCREENSIZE", "Pixel height is " + heightPixels);
        float scaleFactor = metrics.density;
        Log.v("FINDSCREENSIZE", "Scale factor is " + scaleFactor);
        float widthDp = widthPixels / scaleFactor;
        Log.v("FINDSCREENSIZE", "Pixel width dp is " + widthDp);
        float heightDp = heightPixels / scaleFactor;
        Log.v("FINDSCREENSIZE", "Pixel height dp is " + heightDp);
        float smallestWidth = Math.min(widthDp, heightDp);
//        if (axis == "height") {
//            return heightDp;
//        } else {
//            return widthDp;
//        }
        if (axis == "height") {
            return heightPixels;
        } else {
            return widthPixels;
        }
    }

    private boolean isLandscape(){
        return (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

}
