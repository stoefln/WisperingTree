package net.microtrash.wisperingtree.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.bus.LogMessage;
import net.microtrash.wisperingtree.bus.LogReset;
import net.microtrash.wisperingtree.bus.ProgressStatusChange;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;


public class LogFragment extends Fragment {

    private static final String TAG = "LogFragment";

    @InjectView((R.id.log_list_view))
    ListView mListView;

    @InjectView(R.id.log_progress_bar)
    View mProgressBar;

    @InjectView(R.id.log_progress_text)
    TextView mProgressBarText;

    private View mRootView;

    private ArrayList<String> mListLog;
    private ArrayAdapter<String> mAdapter;
    private boolean mTouchScrolling = false;
    private boolean mAutoScroll = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_log, container, false);
        ButterKnife.inject(this, mRootView);

        return mRootView;
    }


    /**
     * called by bus
     */
    public void onEvent(LogMessage message) {
        mListLog.add(message.toString());
        mAdapter.notifyDataSetChanged();
        if (mListView != null && !mTouchScrolling && mAutoScroll) {
            mListView.setSelection(mListView.getCount() - 1);

        }
    }

    /**
     * called by bus
     */
    public void onEvent(LogReset event) {
        mListLog.clear();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * called by bus
     */
    public void onEvent(final ProgressStatusChange event) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                float width = (float) ((View) mProgressBar.getParent()).getWidth() * event.getProgress();
                mProgressBar.getLayoutParams().width = (int) width;
                mProgressBar.requestLayout();
                mProgressBarText.setText(event.getText());
            }
        });
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        mListLog = new ArrayList<String>();

        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mTouchScrolling = true;
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                    mTouchScrolling = false;
                }
                return false;
            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public int preLast;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {


            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                mAutoScroll = false;
                if (lastItem == totalItemCount) {

                    if (preLast != lastItem) { //to avoid multiple calls for last item
                        preLast = lastItem;
                    }
                    mAutoScroll = true;
                }
            }
        });

        int count = new Select()
                .from(LogMessage.class).count();

        final int limit = 2000;
        From select = new Select()
                .from(LogMessage.class).limit(limit);
        if (count > limit) {
            select.offset(count - limit);
        }
        List<LogMessage> result = select.execute();

        for (LogMessage logMessage : result) {
            mListLog.add(logMessage.toString());
        }
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_console, mListLog);
        mListView.setAdapter(mAdapter);
        mListView.setSelection(mListView.getCount() - 1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}

