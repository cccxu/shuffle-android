package org.dodgybits.shuffle.android.list.view.context;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.content.ContextCursorLoader;
import org.dodgybits.shuffle.android.list.event.EditContextEvent;
import org.dodgybits.shuffle.android.list.event.EditListSettingsEvent;
import org.dodgybits.shuffle.android.list.event.EditNewContextEvent;
import org.dodgybits.shuffle.android.list.event.NewContextEvent;
import org.dodgybits.shuffle.android.list.event.QuickAddEvent;
import org.dodgybits.shuffle.android.list.event.UpdateContextDeletedEvent;
import org.dodgybits.shuffle.android.list.event.ViewContextEvent;
import org.dodgybits.shuffle.android.list.event.ViewHelpEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.list.view.QuickAddController;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;

import roboguice.activity.RoboActionBarActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboListFragment;

public class ContextListFragment extends RoboListFragment {
    private static final String TAG = "ContextListFragment";
    
    /** Argument name(s) */
    private static final String BUNDLE_LIST_STATE = "ContextListFragment.state.listState";

    // result codes
    private static final int FILTER_CONFIG = 600;

    @Inject
    private ContextListAdaptor mListAdapter;

    @Inject
    private TaskPersister mTaskPersister;
    
    @Inject
    private ContextPersister mContextPersister;

    @Inject
    private EventManager mEventManager;

    @Inject
    private QuickAddController mQuickAddController;

    private Parcelable mSavedListState;

    private boolean mResumed = false;

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ListView lv = getListView();
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        registerForContextMenu(lv);

        setEmptyText(getString(R.string.no_contexts));

        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
        }

        Log.d(TAG, "-onActivityCreated");
    }

    @Override
    public void onPause() {
        mSavedListState = getListView().onSaveInstanceState();
        super.onPause();

        mResumed = false;
        Log.d(TAG, "-onPause");
    }

    @Override
    public void onResume() {
        super.onResume();

        mResumed = true;
        onVisibilityChange();
        refreshChildCount();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        onVisibilityChange();
    }

    /**
     * Called when a context is clicked.
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        MainView mainView = MainView.newBuilder()
                .setListQuery(ListQuery.context)
                .setEntityId(Id.create(id))
                .build();
        mEventManager.fire(new MainViewUpdateEvent(mainView));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list_menu, menu);

        String addTitle = getString(R.string.menu_insert, getString(R.string.context_name));
        menu.findItem(R.id.action_add).setTitle(addTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Log.d(TAG, "adding task");
                mEventManager.fire(new EditNewContextEvent());
                return true;
            case R.id.action_help:
                Log.d(TAG, "Bringing up help");
                mEventManager.fire(new ViewHelpEvent(ListQuery.context));
                return true;
            case R.id.action_view_settings:
                Log.d(TAG, "Bringing up view settings");
                mEventManager.fire(new EditListSettingsEvent(ListQuery.context, this, FILTER_CONFIG));
                return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        Log.d(TAG, "Got resultCode " + resultCode + " with data " + data);
        switch (requestCode) {
            case FILTER_CONFIG:
                break;

            default:
                Log.e(TAG, ("Unknown requestCode: " + requestCode));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_list_context_menu, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        org.dodgybits.shuffle.android.core.model.Context context = mContextPersister.read(cursor);

        String entityName = getString(R.string.context_name);

        MenuItem deleteMenu = menu.findItem(R.id.action_delete);
        deleteMenu.setVisible(!context.isDeleted());
        deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));

        MenuItem undeleteMenu = menu.findItem(R.id.action_undelete);
        undeleteMenu.setVisible(context.isDeleted());
        undeleteMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!getUserVisibleHint()) return super.onContextItemSelected(item);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_edit:
                mEventManager.fire(new EditContextEvent(Id.create(info.id)));
                return true;
            case R.id.action_delete:
                mEventManager.fire(new UpdateContextDeletedEvent(Id.create(info.id), true));
                mEventManager.fire(new ReloadListCursorEvent());
                return true;
            case R.id.action_undelete:
                mEventManager.fire(new UpdateContextDeletedEvent(Id.create(info.id), false));
                mEventManager.fire(new ReloadListCursorEvent());
                return true;
        }

        return super.onContextItemSelected(item);
    }

    public void onCursorLoaded(@Observes ContextListCursorLoadedEvent event) {
        Log.d(TAG, "Swapping cursor and setting adapter");
        mListAdapter.swapCursor(event.getCursor());
        setListAdapter(mListAdapter);

        // Restore the state -- this step has to be the last, because Some of the
        // "post processing" seems to reset the scroll position.
        if (mSavedListState != null) {
            getListView().onRestoreInstanceState(mSavedListState);
            mSavedListState = null;
        }
    }

    public void onTaskCountCursorLoaded(@Observes ContextTaskCountCursorLoadedEvent event) {
        Cursor cursor = event.getCursor();
        mListAdapter.setTaskCountArray(mTaskPersister.readCountArray(cursor));
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getListView().invalidateViews();
            }
        });
        cursor.close();
    }

    private void onVisibilityChange() {
        if (getUserVisibleHint()) {
            updateTitle();
            updateQuickAdd();
            getRoboActionBarActivity().supportInvalidateOptionsMenu();
        }
    }

    protected RoboActionBarActivity getRoboActionBarActivity() {
        return (RoboActionBarActivity) getActivity();
    }

    private void updateTitle() {
        getActivity().setTitle(R.string.title_context);
    }

    public void onQuickAddEvent(@Observes QuickAddEvent event) {
        if (getUserVisibleHint() && mResumed) {
            mEventManager.fire(new NewContextEvent(event.getValue()));
        }
    }

    private void updateQuickAdd() {
        mQuickAddController.init(getActivity());
        mQuickAddController.setEnabled(ListSettingsCache.findSettings(ListQuery.context).getQuickAdd(getActivity()));
        mQuickAddController.setEntityName(getString(R.string.context_name));
    }

    void restoreInstanceState(Bundle savedInstanceState) {
        mSavedListState = savedInstanceState.getParcelable(BUNDLE_LIST_STATE);
    }


    private void refreshChildCount() {
        mEventManager.fire(new ReloadCountCursorEvent());
    }

}