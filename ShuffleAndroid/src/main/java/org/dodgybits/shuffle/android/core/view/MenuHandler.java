package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.MainViewUpdateEvent;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.list.event.EditListSettingsEvent;
import org.dodgybits.shuffle.android.list.event.EditNewContextEvent;
import org.dodgybits.shuffle.android.list.event.EditNewProjectEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class MenuHandler {
    private static final String TAG = "MenuHandler";

    // result codes
    private static final int FILTER_CONFIG = 600;

    private ActionBarActivity mActivity;

    private MainView mMainView;

    private TaskListContext mTaskListContext;

    @Inject
    private EventManager mEventManager;

    @Inject
    EntityCache<Context> mContextCache;

    @Inject
    EntityCache<Project> mProjectCache;

    @Inject
    public MenuHandler(Activity activity) {
        mActivity = (ActionBarActivity) activity;
    }

    public boolean onCreateOptionsMenu(Menu menu, MenuInflater inflater, boolean drawerOpen) {
        if (mMainView == null) {
            return false;
        }

        switch (mMainView.getViewMode()) {
            case CONTEXT_LIST:
                addListMenu(menu, inflater, getString(R.string.context_name));
                break;
            case PROJECT_LIST:
                addListMenu(menu, inflater, getString(R.string.project_name));
                break;
            case TASK_LIST:
                addListMenu(menu, inflater, getString(R.string.task_name));
                break;
            case TASK:
                inflater.inflate(R.menu.task_view_menu, menu);
                break;
            case SEARCH_RESULTS_TASK:
            case SEARCH_RESULTS_LIST:
                break;
        }

        return true;
    }

    private void addListMenu(Menu menu, MenuInflater inflater, String entityName) {
        inflater.inflate(R.menu.list_menu, menu);
        String addTitle = getString(R.string.menu_insert, entityName);
        menu.findItem(R.id.action_add).setTitle(addTitle);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mMainView == null) {
            return false;
        }

        switch (mMainView.getViewMode()) {
            case CONTEXT_LIST:
                break;
            case PROJECT_LIST:
                break;
            case TASK_LIST:
                TaskListContext listContext = TaskListContext.create(mMainView);
                if (listContext == null) {
                    return false;
                }

                String taskName = getString(R.string.task_name);
                String addTitle = getString(R.string.menu_insert, taskName);
                menu.findItem(R.id.action_add).setTitle(addTitle);

                MenuItem editMenu = menu.findItem(R.id.action_edit);
                MenuItem deleteMenu = menu.findItem(R.id.action_delete);
                MenuItem restoreMenu = menu.findItem(R.id.action_undelete);
                if (listContext.showEditActions()) {
                    String entityName = listContext.getEditEntityName(mActivity);
                    boolean entityDeleted = listContext.isEditEntityDeleted(mActivity, mContextCache, mProjectCache);
                    editMenu.setVisible(true);
                    editMenu.setTitle(getString(R.string.menu_edit, entityName));
                    deleteMenu.setVisible(!entityDeleted);
                    deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));
                    restoreMenu.setVisible(entityDeleted);
                    restoreMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));
                } else {
                    editMenu.setVisible(false);
                    deleteMenu.setVisible(false);
                    restoreMenu.setVisible(false);
                }
                break;
            case TASK:
                break;
            case SEARCH_RESULTS_TASK:
            case SEARCH_RESULTS_LIST:
                break;
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (mMainView.getViewMode() == ViewMode.CONTEXT_LIST) {
                    mEventManager.fire(new EditNewContextEvent());
                } else if (mMainView.getViewMode() == ViewMode.PROJECT_LIST) {
                    mEventManager.fire(new EditNewProjectEvent());
                } else {
                    mEventManager.fire(mTaskListContext.createEditNewTaskEvent());
                }
                return true;
            case R.id.action_view_settings:
                Log.d(TAG, "Bringing up view settings");
                mEventManager.fire(
                        new EditListSettingsEvent(mMainView.getListQuery(), mActivity, FILTER_CONFIG));
                return true;
            case R.id.action_edit:
                mEventManager.fire(mTaskListContext.createEditEvent());
                return true;
            case R.id.action_delete:
                mEventManager.fire(mTaskListContext.createDeleteEvent(true));
                // TODO - go back to parent view
                mActivity.finish();
                return true;
            case R.id.action_undelete:
                mEventManager.fire(mTaskListContext.createDeleteEvent(false));
                // TODO - go back to parent view
                mActivity.finish();
                return true;
        }
        return false;
    }

    private void onViewChanged(@Observes MainViewUpdateEvent event) {
        mMainView = event.getMainView();
        mTaskListContext = TaskListContext.create(mMainView);
    }

    private String getString(int id) {
        return mActivity.getString(id);
    }

    private String getString(int id, String arg) {
        return mActivity.getString(id, arg);
    }
}
