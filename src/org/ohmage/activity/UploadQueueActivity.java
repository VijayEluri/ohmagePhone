package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Response;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

public class UploadQueueActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "UploadQueueActivity";
	private ContentResolver mCR;
	private FilterControl mCampaignFilter;
	private Button mUploadAll;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.upload_queue_layout);

		// Add the surveys to the list fragment
		final FragmentManager fm = getSupportFragmentManager();

		// instantiate a filter just for fun and populate it from the campaigns list
		// also create a filter that will be populated by the survey list
		mCampaignFilter = (FilterControl) findViewById(R.id.campaign_filter);
		mCampaignFilter.setVisibility(View.GONE);

		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				if (getUploadingResponseListFragment() != null) {
					getUploadingResponseListFragment().setFilters(curValue, null);
				}
			}
		});

		if (fm.findFragmentById(R.id.root_container) == null) {

			UploadingResponseListFragment list = new UploadingResponseListFragment();

			fm.beginTransaction().add(R.id.root_container, list, "list").commit();
		}

		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		mUploadAll = (Button) findViewById(R.id.upload_button);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Campaign.getCampaigns(), new String [] { Campaign.URN, Campaign.NAME }, 
				Campaign.STATUS + "=" + Campaign.STATUS_READY, null, Campaign.NAME);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Now that the campaigns loaded, we can show the filters
		mCampaignFilter.setVisibility(View.VISIBLE);

		// Populate the filter
		mCampaignFilter.populate(data, Campaign.NAME, Campaign.URN);
		mCampaignFilter.add(0, new Pair<String,String>("All Campaigns", null));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCampaignFilter.clearAll();
	}

	private ResponseListFragment getUploadingResponseListFragment() {
		return (UploadingResponseListFragment) getSupportFragmentManager().findFragmentByTag("list");
	}

	public static class UploadingResponseListFragment extends ResponseListFragment {

		@Override
		protected ResponseListCursorAdapter createAdapter() {
			return new UploadingResponseListCursorAdapter(getActivity(), null, 0);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			CursorLoader loader = (CursorLoader) super.onCreateLoader(id, args);

			StringBuilder selection = new StringBuilder(loader.getSelection());
			if(selection.length() != 0)
				selection.append(" AND ");
			selection.append(Response.SOURCE  + "='local'");
			loader.setSelection(selection.toString());
			return loader;
		}
	}
}
