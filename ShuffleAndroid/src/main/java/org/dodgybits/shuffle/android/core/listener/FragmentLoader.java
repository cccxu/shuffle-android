/**
 * Copyright (C) 2014 Android Shuffle Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.view.context.ContextListFragment;
import org.dodgybits.shuffle.android.list.view.project.ProjectListFragment;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.list.view.task.TaskListFragment;
import org.dodgybits.shuffle.android.view.fragment.TaskPagerFragment;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class FragmentLoader {
    private static final String TAG = "FragmentLoader";

    /** Tags used when loading fragments. */
    public static final String TAG_TASK_LIST = "tag-task-list";
    public static final String TAG_TASK_ITEM = "tag-task-item";
    public static final String TAG_CONTEXT_LIST = "tag-context-list";
    public static final String TAG_PROJECT_LIST = "tag-project-list";

    private MainView mMainView;

    private FragmentActivity mActivity;

    private Boolean tabletUi;

    @Inject
    private EventManager mEventManager;

    @Inject
    public FragmentLoader(Activity activity) {
        mActivity = (FragmentActivity) activity;
    }

    private boolean isTabletUi() {
        if (tabletUi == null) {
            tabletUi = UiUtilities.useTabletUI(mActivity.getResources());
        }
        return tabletUi;
    }

    private void onViewUpdated(@Observes ViewUpdatedEvent event) {
        mMainView = event.getMainView();
    }

    private void onTaskListCursorLoaded(@Observes TaskListCursorLoadedEvent event) {
        Log.i(TAG, "Task list Cursor loaded - loading fragment now");
        switch (mMainView.getViewMode()) {
            case TASK_LIST:
                addTaskList(event.getTaskListContext());
                break;
            case TASK:
                if (isTabletUi()) {
                    addTaskList(event.getTaskListContext());
                }
                addTaskView(event.getTaskListContext());
                break;
            default:
                Log.w(TAG, "Unexpected view mode " + mMainView.getViewMode());
                break;
        }
        mEventManager.fire(new ViewUpdatedEvent(mMainView));
    }

    private void onContextListCursorLoaded(@Observes ContextListCursorLoadedEvent event) {
        if (mMainView.getViewMode() == ViewMode.CONTEXT_LIST) {
            addContextList();
        }
        mEventManager.fire(new ViewUpdatedEvent(mMainView));
    }

    private void onProjectListCursorLoaded(@Observes ProjectListCursorLoadedEvent event) {
        if (mMainView.getViewMode() == ViewMode.PROJECT_LIST) {
            addProjectList();
        }
        mEventManager.fire(new ViewUpdatedEvent(mMainView));
    }

    private void addTaskList(TaskListContext listContext) {
        TaskListFragment fragment = getTaskListFragment();
        if (fragment == null) {
            fragment = new TaskListFragment();
            Log.d(TAG, "Creating task list fragment " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            fragmentTransaction.replace(isTabletUi() ? R.id.entity_list_pane : R.id.main_pane,
                    fragment, TAG_TASK_LIST);
            fragmentTransaction.commit();
        }
    }

    private void addTaskView(TaskListContext listContext) {
        TaskPagerFragment fragment = getTaskPagerFragment() ;
        if (fragment == null) {
            fragment = new TaskPagerFragment();
            Log.d(TAG, "Creating task pager " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            fragmentTransaction.replace(isTabletUi() ? R.id.task_pane : R.id.main_pane,
                    fragment, TAG_TASK_ITEM);
            fragmentTransaction.commit();
        }
    }

    private void addContextList() {
        ContextListFragment fragment = getContextListFragment();
        if (fragment == null) {
            fragment = new ContextListFragment();
            Log.d(TAG, "Creating context list fragment " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            fragmentTransaction.replace(isTabletUi() ? R.id.entity_list_pane : R.id.main_pane,
                    fragment,
                    TAG_CONTEXT_LIST);
            fragmentTransaction.commit();
        }
    }

    private void addProjectList() {
        ProjectListFragment fragment = getProjectListFragment();
        if (fragment == null) {
            fragment = new ProjectListFragment();
            Log.d(TAG, "Creating project list fragment " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            fragmentTransaction.replace(isTabletUi() ? R.id.entity_list_pane : R.id.main_pane,
                    fragment,
                    TAG_PROJECT_LIST);
            fragmentTransaction.commit();
        }
    }

    /**
     * Get the task list fragment for this activity. If the task list fragment is
     * not attached, this method returns null.
     *
     * Caution! This method returns the {@link TaskListFragment} after the fragment has been
     * added, <b>and</b> after the {@link android.app.FragmentManager} has run through its queue to add the
     * fragment. There is a non-trivial amount of time after the fragment is instantiated and before
     * this call returns a non-null value, depending on the {@link android.app.FragmentManager}. If you
     * need the fragment immediately after adding it, consider making the fragment an observer of
     * the controller and perform the task immediately on {@link android.app.Fragment#onActivityCreated(android.os.Bundle)}
     */
    protected TaskListFragment getTaskListFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_TASK_LIST);
        if (isValidFragment(fragment)) {
            return (TaskListFragment) fragment;
        }
        return null;
    }

    protected TaskPagerFragment getTaskPagerFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_TASK_ITEM);
        if (isValidFragment(fragment)) {
            return (TaskPagerFragment) fragment;
        }
        return null;
    }

    protected ContextListFragment getContextListFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_CONTEXT_LIST);
        if (isValidFragment(fragment)) {
            return (ContextListFragment) fragment;
        }
        return null;
    }

    protected ProjectListFragment getProjectListFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_PROJECT_LIST);
        if (isValidFragment(fragment)) {
            return (ProjectListFragment) fragment;
        }
        return null;
    }

    /**
     * Check if the fragment is attached to an activity and has a root view.
     * @param in fragment to be checked
     * @return true if the fragment is valid, false otherwise
     */
    private static boolean isValidFragment(Fragment in) {
        return !(in == null || in.getActivity() == null || in.getView() == null);
    }

}
