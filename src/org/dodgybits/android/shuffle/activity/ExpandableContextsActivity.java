package org.dodgybits.android.shuffle.activity;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.android.shuffle.model.Context;
import org.dodgybits.android.shuffle.model.Task;
import org.dodgybits.android.shuffle.provider.Shuffle;
import org.dodgybits.android.shuffle.util.MenuUtils;
import org.dodgybits.android.shuffle.util.BindingUtils;
import org.dodgybits.android.shuffle.view.ContextView;
import org.dodgybits.android.shuffle.view.ExpandableContextView;
import org.dodgybits.android.shuffle.view.ExpandableTaskView;
import org.dodgybits.android.shuffle.view.TaskView;

import android.database.Cursor;
import android.net.Uri;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;

public class ExpandableContextsActivity extends AbstractExpandableActivity<Context, Task> {
    private int mChildIdColumnIndex; 
    private int mGroupIdColumnIndex; 
	private SparseIntArray mTaskCountArray;
	
	@Override
	protected void refreshChildCount() {
		Cursor cursor = getContentResolver().query(
				Shuffle.Contexts.cContextTasksContentURI, 
				Shuffle.Contexts.cFullTaskProjection, null, null, null);
		mTaskCountArray = BindingUtils.readCountArray(cursor);
		cursor.close();
	}
	

	@Override
	protected int getContentViewResId() {
		return R.layout.expandable_contexts;
	}
	
	@Override
	protected Uri getGroupContentUri() {
		return Shuffle.Contexts.CONTENT_URI;
	}

	@Override
	protected Uri getChildContentUri() {
		return Shuffle.Tasks.CONTENT_URI;
	}

	@Override
	protected Cursor createGroupQuery() {
		Cursor cursor = managedQuery(Shuffle.Contexts.CONTENT_URI, Shuffle.Contexts.cFullProjection,
				null, null, Shuffle.Contexts.NAME + " ASC");
		mGroupIdColumnIndex = cursor.getColumnIndex(Shuffle.Contexts._ID);
		return cursor;
	}

	@Override
	protected int getGroupIdColumnIndex() {
		return mGroupIdColumnIndex;
	}
	
	@Override
	protected int getChildIdColumnIndex() {
		return mChildIdColumnIndex;
	}


	@Override
	protected Cursor createChildQuery(long groupId) {
		Cursor cursor = managedQuery(Shuffle.Tasks.CONTENT_URI, Shuffle.Tasks.cExpandedProjection,
				Shuffle.Tasks.CONTEXT_ID + " = ?", new String[] {String.valueOf(groupId)}, 
				Shuffle.Tasks.CREATED_DATE + " ASC");
		mChildIdColumnIndex = cursor.getColumnIndex(Shuffle.Tasks._ID);
		return cursor;
	}

	@Override
    protected int getCurrentViewMenuId() {
    	return MenuUtils.CONTEXT_ID;
    }

	@Override
	protected String getChildName() {
		return getString(R.string.task_name);
	}

	@Override
	protected String getGroupName() {
		return getString(R.string.context_name);
	}
	
	@Override
	protected String getGroupIdColumnName() {
		return Shuffle.Tasks.CONTEXT_ID;
	}

	@Override
	protected ExpandableListAdapter createExpandableListAdapter(Cursor cursor) {
		return new MyExpandableListAdapter(this, 
        		cursor,
                android.R.layout.simple_expandable_list_item_1,
                android.R.layout.simple_expandable_list_item_1,
                new String[] {Shuffle.Contexts.NAME}, 
                new int[] {android.R.id.text1},
                new String[] {Shuffle.Tasks.DESCRIPTION},
                new int[] {android.R.id.text1}) {

	        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
	        	Cursor cursor = (Cursor) getChild(groupPosition, childPosition);
				Task task = readChild(cursor);
				TaskView taskView;
				if (convertView instanceof ExpandableTaskView) {
					taskView = (ExpandableTaskView) convertView;
				} else {
					taskView = new ExpandableTaskView(parent.getContext(), false);
				}
				boolean isSelected = false;
				taskView.updateView(task, isSelected);
				return taskView;
	        }

	        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
	        	Cursor cursor = (Cursor) getGroup(groupPosition);
	        	Context context = readGroup(cursor);
				ContextView contextView;
				if (convertView instanceof ExpandableContextView) {
					contextView = (ExpandableContextView) convertView;
				} else {
					contextView = new ExpandableContextView(parent.getContext());
				}
				contextView.setTaskCountArray(mTaskCountArray);
				boolean isSelected = false;
				contextView.updateView(context, isSelected);
				return contextView;
	        }
			
		};
	}
	@Override
	Task readChild(Cursor cursor) {
        return BindingUtils.readTask(cursor);
	}

	@Override
	Context readGroup(Cursor cursor) {
        return BindingUtils.readContext(cursor);
	}


}