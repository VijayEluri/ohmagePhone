package org.ohmage.activity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.OhmageApi.CampaignXmlResponse;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Response;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

public class CampaignInfoActivity extends BaseInfoActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	// helpers
	private FragmentActivity mContext;
	private SharedPreferencesHelper mSharedPreferencesHelper;
	
	// action bar commands
	private static final int ACTION_TAKE_SURVEY = 1;
	private static final int ACTION_VIEW_RESPHISTORY = 2;
	
	// handles to views we'll be manipulating
	private TextView mErrorBox;
	private TextView mDescView;
	private TextView mPrivacyValue;
	private TextView mStatusValue;
	private TextView mResponsesValue;
	
	// state vars
	private int mCampaignStatus; // status code for campaign as of last refresh

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// save the context so the action bar can use it to fire off intents
		mContext = this;
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
		getActionBar().setTitle("Campaign Info");
		
		// inflate the campaign-specific info page into the scrolling framelayout
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.campaign_info_details, getContentArea(), true);
		
		// and inflate all the possible commands into the button tray
		inflater.inflate(R.layout.campaign_info_buttons, mButtonTray, true);
		
		// clear some things to their default values
		mNotetext.setVisibility(View.GONE);
		
		// nab references to things we'll be populating
		mErrorBox = (TextView)findViewById(R.id.campaign_info_errorbox);
		mDescView = (TextView)findViewById(R.id.campaign_info_desc);
		
		mPrivacyValue = (TextView)findViewById(R.id.campaign_info_privacy_value);
		mStatusValue = (TextView)findViewById(R.id.campaign_info_status_value);
		mResponsesValue = (TextView)findViewById(R.id.campaign_info_responses_value);
		
		// and attach some handlers + populate some html data
		// this one's for the privacy section
		TextView privacyDetails = (TextView)findViewById(R.id.campaign_info_privacy_details);
		privacyDetails.setText(Html.fromHtml(getString(R.string.campaign_info_privacy_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.campaign_info_privacy_row),
				privacyDetails);
		
		// and this one is for the responses section
		TextView responsesDetails = (TextView)findViewById(R.id.campaign_info_responses_details);
		responsesDetails.setText(Html.fromHtml(getString(R.string.campaign_info_responses_details)));
		setDetailsExpansionHandler(
				findViewById(R.id.campaign_info_responses_row),
				responsesDetails);
		
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getSupportLoaderManager().initLoader(0, null, this);
	}
	
	protected void populateCommands(final String campaignUrn, final int campaignStatus) {
		// first remove all the commands from the action bar...
		ActionBarControl actionBar = getActionBar();
		actionBar.clearActionBarCommands();
		
		// ...and gather up the commands in the command tray so we can hide/show them
		Button participateButton = (Button)findViewById(R.id.campaign_info_button_particpate);
		Button removeButton = (Button)findViewById(R.id.campaign_info_button_remove);
		
		// now, depending on the context, we can regenerate our commands
		// this applies both to the action bar and to the command tray
		if (campaignStatus != Campaign.STATUS_REMOTE) {
			actionBar.addActionBarCommand(ACTION_TAKE_SURVEY, "take survey", R.drawable.dashboard_title_survey);
			actionBar.addActionBarCommand(ACTION_VIEW_RESPHISTORY, "take survey", R.drawable.dashboard_title_resphist);
			
			// route the actions to the appropriate places
			actionBar.setOnActionListener(new ActionListener() {
				@Override
				public void onActionClicked(int commandID) {
					Intent intent;
					
					switch (commandID) {
						case ACTION_TAKE_SURVEY:
							intent = new Intent(mContext, SurveyListActivity.class);
							intent.putExtra("campaign_urn", campaignUrn);
							startActivity(intent);
							break;
						case ACTION_VIEW_RESPHISTORY:
							intent = new Intent(mContext, RHTabHost.class);
							intent.putExtra("campaign_urn", campaignUrn);
							startActivity(intent);
							break;
					}
				}
			});
			
			// and set the command tray buttons accordingly
			participateButton.setVisibility(View.GONE);
			removeButton.setVisibility(View.VISIBLE);
			
			// attach a remove handler
			removeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setMessage("Are you sure that you want to remove this campaign? Any data that you haven't uploaded will be lost!")
						.setCancelable(false)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// remove the campaign URN and take us back to my campaigns
								getContentResolver().delete(Campaign.CONTENT_URI, Campaign.URN + "=?", new String[]{campaignUrn});
								startActivity(new Intent(mContext, CampaignListActivity.class));
								mContext.finish();
							}
						})
						.setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						AlertDialog alert = builder.create();
						alert.show();
				}
			});
		}
		else {
			// show commands for a remote campaign (e.g. "participate")
			participateButton.setVisibility(View.VISIBLE);
			removeButton.setVisibility(View.GONE);
			
			// attach a participation handler
			participateButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// when clicked, it fires off a download task,
					// waits for it to finish, then goes back to the list when it's done
					(new CampaignXmlDownloadTask(mContext, campaignUrn) {
						@Override
						protected void onPostExecute(CampaignXmlResponse response) {
							// TODO Auto-generated method stub
							super.onPostExecute(response);
							
							// take us to my campaigns so we can see the entry in all its newfound glory
							/*
							startActivity(new Intent(mContext, CampaignListActivity.class));
							mContext.finish();
							*/
						}
					})
					.execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());
				}
			});
		}
	}
	
	// ========================================================
	// === view databinding below,
	// === describes how this info view shows its data
	// ========================================================

	private interface QueryParams {
		String[] PROJECTION = {
					Campaign.URN,
					Campaign.NAME,
					Campaign.CONFIGURATION_XML,
					Campaign.DESCRIPTION,
					Campaign.STATUS,
					Campaign.PRIVACY
				};
		
		final int URN = 0;
		final int NAME = 1;
		final int CONFIGURATION_XML = 2;
		final int DESCRIPTION = 3;
		final int STATUS = 4;
		final int PRIVACY = 5;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(this, getIntent().getData(), QueryParams.PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// ensure that we're on the first record in the cursor
		if (!data.moveToFirst())
			return;

		// populate the views
		try {
			String campaignUrn = data.getString(QueryParams.URN);
			String xmlData = data.getString(QueryParams.CONFIGURATION_XML);
			
			if (xmlData != null) {
				// nab the data from the xml associated with this campaign
				Map<String, String> campaignInfo = PromptXmlParser.parseCampaignInfo(
						new ByteArrayInputStream(xmlData.getBytes("UTF-8"))
						);
			}

			// set the header fields first
			mHeadertext.setText(data.getString(QueryParams.NAME));
			mSubtext.setText(campaignUrn);
			mNotetext.setVisibility(View.INVISIBLE);
			
			// fill in the description
			mDescView.setText(data.getString(QueryParams.DESCRIPTION));
			
			// set the appropriate text and icon for the privacy state
			String privacy = data.getString(QueryParams.PRIVACY);
			mPrivacyValue.setText(privacy);
			if (privacy.equalsIgnoreCase("private"))
				mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_private, 0, 0, 0);
			else if (privacy.equalsIgnoreCase("shared"))
				mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_shared, 0, 0, 0);
			else
				mPrivacyValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_private, 0, 0, 0);
			
			// hide our error box; it'll become visible below (and filled w/text) if the status is appropriate
			mErrorBox.setVisibility(View.GONE);
			
			// set many things on the view according to the campaign status, too
			mStatusValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // start out with nothing drawn
			mCampaignStatus = data.getInt(QueryParams.STATUS);
			switch (mCampaignStatus) {
				case Campaign.STATUS_READY:
					mStatusValue.setText("participating");
					mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_running, 0, 0, 0);
					break;
				case Campaign.STATUS_VAGUE:
					mStatusValue.setText("not available");
					break;
				case Campaign.STATUS_REMOTE:
					mStatusValue.setText("available");
					break;
				case Campaign.STATUS_OUT_OF_DATE:
					mStatusValue.setText("out of date");
					break;
				case Campaign.STATUS_DELETED:
					mStatusValue.setText("deleted on server");
					break;
				case Campaign.STATUS_STOPPED:
					mStatusValue.setText("stopped");
					mStatusValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.website_stopped, 0, 0, 0);
					mErrorBox.setVisibility(View.VISIBLE);
					mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_stopped)));
					break;
				case Campaign.STATUS_INVALID_USER_ROLE:
					mStatusValue.setText("invalid role");
					mErrorBox.setVisibility(View.VISIBLE);
					mErrorBox.setText(Html.fromHtml(getString(R.string.campaign_info_errorbox_invalid_role)));
					break;
				case Campaign.STATUS_DOWNLOADING:
					mStatusValue.setText("downloading...");
					break;
				default:
					mStatusValue.setText("unknown status");
					break;
			}
			
			// set the responses by querying the response table
			// and getting the number of responses submitted for this campaign
			Cursor responses = getContentResolver().query(Response.getResponsesByCampaign(campaignUrn), null, null, null, null);
			mResponsesValue.setText(responses.getCount() + " response(s) submitted");
			
			// and finally populate the action bar + command tray
			populateCommands(campaignUrn, mCampaignStatus);

			// and make the entity header visible (although i assume it already was)
			mEntityHeader.setVisibility(View.VISIBLE);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// FIXME should we hide the entity header like cameron does?
	}
}