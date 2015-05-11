package org.dodgybits.shuffle.android.list.activity;

import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;

public class TaskListActivity extends AbstractMainActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskList;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (mActionBarToolbar != null) {
            ViewCompat.setElevation(mActionBarToolbar, 0f);
        }
    }

    @Override
    protected boolean handleBackPress() {
        if (mLocation != null && mLocation.getViewMode() == ViewMode.TASK && UiUtilities.isListCollapsible(getResources())) {
            mEventManager.fire(new NavigationRequestEvent(mLocation.parent()));
            return true;
        }
        return false;
    }
}