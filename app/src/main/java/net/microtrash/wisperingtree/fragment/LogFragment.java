package net.microtrash.wisperingtree.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.bus.LogMessage;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;


public class LogFragment extends Fragment {

    private static final String TAG = "LogFragment";

    @InjectView((R.id.log_list_view))
    ListView mListView;

    private View mRootView;

    private ArrayList<String> mListLog;
    private ArrayAdapter<String> mAdapter;

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
    public void onEvent(LogMessage message){
        mListLog.add(message.getText());
        mAdapter.notifyDataSetChanged();
        if(mListView != null) {
            mListView.setSelection(mListView.getCount() - 1);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        mListLog = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_console, mListLog);
        mListView.setAdapter(mAdapter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}

