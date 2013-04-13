package tankery.app.family.photos.widget;

import java.lang.ref.WeakReference;
import tankery.app.family.photos.R;
import tankery.app.family.photos.data.PhotoList;
import tankery.app.family.photos.data.PhotoList.PhotoListError;
import tankery.app.family.photos.data.PhotoList.PhotoListListener;
import tankery.app.modules.waterfall.data.CachedBitmap;
import tankery.app.modules.waterfall.widget.WaterfallView;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class FamilyPhotosView extends WaterfallView
                              implements PhotoListListener {

    private static final String LOGTAG = "FamilyPhotosView";

    private enum WaterfallErrorType {
        UNKNOW_ERROR,
        PHOTO_FETCHING_TIMEOUT
    }

    private static final int PHOTO_FETCHING_COUNT = 30;

    private PhotoList mPhotoList;

    public FamilyPhotosView(Context context) {
        super(context);
    }

    public FamilyPhotosView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FamilyPhotosView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void init(int column) {
        super.init(column);
        initPhotoList();
    }

    private void initPhotoList() {
        mPhotoList = new PhotoList(getContext());
        mPhotoList.setPhotoCompressedWidth(getColumnWidth());
        mPhotoList.setFetchingFinishedListener(this);
        mPhotoList.refreshPhotoList();
        // append new item at initial.
        needAppendNewItems();
    }

    private final WaterfallErrorHandler waterfallErrorHandler =
            new WaterfallErrorHandler(this);

    private final static class WaterfallErrorHandler extends Handler {

        private WeakReference<FamilyPhotosView> mView;

        WaterfallErrorHandler(FamilyPhotosView view) {
            mView = new WeakReference<FamilyPhotosView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            final WaterfallErrorType types[] = WaterfallErrorType.values();
            if (msg.what >= types.length) {
                Log.e(LOGTAG, "msg.what =" + msg.what +
                           " is out of WaterfallErrorType.");
                return;
            }
            switch (types[msg.what]) {
            case UNKNOW_ERROR:
                mView.get().showUserMessage((String) msg.obj);
                break;
            case PHOTO_FETCHING_TIMEOUT:
                mView.get().showUserMessage(R.string.app_err_connection_timeout);
                break;
            default:
                Log.e(LOGTAG, types[msg.what].name() + " is invalidate.");
                break;
            }
        }
    }

    // Note this function can only calling in UI thread.
    private void needAppendNewItems() {
        mPhotoList.fetchMorePhotos(PHOTO_FETCHING_COUNT);
    }

    public void showUserMessage(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public void showUserMessage(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }


    /**************************************************
     * WaterfallView Overrides
     **************************************************/

    @Override
    public void onTopReached() {
        Log.d(LOGTAG, "Scrolled to top");
        showUserMessage(R.string.waterfall_refresh);
        mPhotoList.refreshPhotoList();
        super.onTopReached();
    }

    @Override
    public void onBottomReached() {
        Log.d(LOGTAG, "Scrolled to bottom");
        showUserMessage(R.string.waterfall_adding_item);
        needAppendNewItems();
        super.onBottomReached();
    }


    /**************************************************
     * PhotoListListener Implements
     **************************************************/

    @Override
    public void onListFetchingFinished() {
        needAppendNewItems();
    }

    @Override
    public void onPhotoReceived(CachedBitmap cbmp) {
        super.appendBitmap(cbmp);
    }

    @Override
    public void onErrorOccurred(PhotoListError err) {
        switch (err) {
        case HTTP_CONNETION_TIMEOUT:
            int what = WaterfallErrorType.PHOTO_FETCHING_TIMEOUT.ordinal();
            waterfallErrorHandler.obtainMessage(what)
                                 .sendToTarget();
            break;
        default:
            what = WaterfallErrorType.UNKNOW_ERROR.ordinal();
            waterfallErrorHandler.obtainMessage(what, err.name())
                                 .sendToTarget();
            break;
        }
    }

}
