package net.microtrash.wisperingtree.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.bus.LogMessage;
import net.microtrash.wisperingtree.bus.ProgressStatusChange;

import java.util.ArrayList;

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
        mListLog.add(message.getText());
        mAdapter.notifyDataSetChanged();
        if (mListView != null && !mTouchScrolling) {
            mListView.setSelection(mListView.getCount() - 1);
        }
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
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_console, mListLog);
        mListView.setAdapter(mAdapter);
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

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}

