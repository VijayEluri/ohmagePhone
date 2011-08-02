package org.ohmage.feedback;

import org.ohmage.feedback.FeedbackContract.FeedbackPromptResponses;
import org.ohmage.feedback.FeedbackContract.FeedbackResponses;
import org.ohmage.feedback.FeedbackContract.FeedbackPromptResponses.AggregateTypes;
import org.ohmage.feedback.FeedbackDatabase.Tables;
import org.ohmage.feedback.utils.SelectionBuilder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class FeedbackProvider extends ContentProvider {		
	private static UriMatcher sUriMatcher = buildUriMatcher();
	private FeedbackDatabase dbHelper;
	
	// enum of the URIs we can match using sUriMatcher
	private interface MatcherTypes {
		int RESPONSES = 1;
		int RESPONSE_BY_PID = 2;
		int CAMPAIGN_SURVEY_RESPONSES = 3;
		int RESPONSE_PROMPTS = 4;
		int CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID = 5;
		int PROMPTS = 6;
		int PROMPT_BY_PID = 7;
		int CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE = 8;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new FeedbackDatabase(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        	
            case MatcherTypes.RESPONSES:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES:
            	return FeedbackResponses.CONTENT_TYPE;
            case MatcherTypes.RESPONSE_BY_PID:
            	return FeedbackResponses.CONTENT_ITEM_TYPE;
            	
            case MatcherTypes.PROMPTS:
            case MatcherTypes.RESPONSE_PROMPTS:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID:
            case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE:
            	return FeedbackPromptResponses.CONTENT_TYPE;
            case MatcherTypes.PROMPT_BY_PID:
            	return FeedbackPromptResponses.CONTENT_ITEM_TYPE;
            	
            default:
                throw new UnsupportedOperationException("getType(): Unknown URI: " + uri);
        }
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// get a handle to our db
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		// feed the uri to our selection builder, which will
		// nab the appropriate rows from the right table.
		SelectionBuilder builder = buildSelection(uri);

		builder.where(selection, selectionArgs);
		
		Cursor result = builder.query(db, projection, sortOrder);
		
		return result;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// insertion is limited to responses, so we just check for that directly and do the insert ourselves
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long insertID = -1;
		
		switch (sUriMatcher.match(uri)) {
			case MatcherTypes.RESPONSES:
				break;
			default:
				throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
		}
		
		db.close();
		
		getContext().getContentResolver().notifyChange(uri, null);
		
		// return the path to our new URI
		return FeedbackResponses.getResponseUri(insertID);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// we don't support updating for now
		return 0;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// get a handle to our db
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		
		// feed the uri to our selection builder, which will
		// nab the appropriate rows from the right table.
		SelectionBuilder builder = buildSelection(uri);
		
		// we should also add on the client's selection, perhaps?
		builder.where(selection, selectionArgs);
		
		// TODO: possibly filter out deletions that shouldn't be performed?
		
		// we assume we've matched it correctly, so proceed with the delete
		count = builder.delete(db);
		
		db.close();
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	// ====================================
	// === definitions for URI resolver and entity type maps
	// ====================================
	
	private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "responses", MatcherTypes.RESPONSES);
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "responses/#", MatcherTypes.RESPONSE_BY_PID);
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "responses/#/prompts", MatcherTypes.RESPONSE_PROMPTS);
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "prompts", MatcherTypes.PROMPTS);
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "prompts/#", MatcherTypes.PROMPT_BY_PID);
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "*/*/responses", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES);
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "*/*/responses/prompts/*", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID);
        matcher.addURI(FeedbackContract.CONTENT_AUTHORITY, "*/*/responses/prompts/*/*", MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE);
        return matcher;
    }
	
	private SelectionBuilder buildSelection(Uri uri) {
		final SelectionBuilder builder = new SelectionBuilder();
		
		// vars used below
		// defined here because case statements are reasonably not scoped
		String campaignUrn, surveyID, responseID, promptID;
		
		final int match = sUriMatcher.match(uri);
		
		switch (match) {
			case MatcherTypes.RESPONSES:
				return builder.table(Tables.RESPONSES);
				
			case MatcherTypes.RESPONSE_BY_PID:
				responseID = uri.getPathSegments().get(1);
				
				return builder.table(Tables.RESPONSES)
					.where(FeedbackResponses._ID + "=?", responseID);

			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES:
				campaignUrn = uri.getPathSegments().get(0);
				surveyID = uri.getPathSegments().get(1);
				
				return builder.table(Tables.RESPONSES)
					.where(FeedbackResponses.CAMPAIGN_URN + "=?", campaignUrn)
					.where(FeedbackResponses.SURVEY_ID + "=?", surveyID);
				
			case MatcherTypes.RESPONSE_PROMPTS:
				responseID = uri.getPathSegments().get(1);
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES)
					.mapToTable(FeedbackPromptResponses._ID, Tables.PROMPTS)
					.mapToTable(FeedbackPromptResponses.RESPONSE_ID, Tables.PROMPTS)
					.where(Tables.RESPONSES + "." + FeedbackResponses._ID + "=?", responseID);
				
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID:
				campaignUrn = uri.getPathSegments().get(0);
				surveyID = uri.getPathSegments().get(1);
				promptID = uri.getPathSegments().get(4);
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES)
					.mapToTable(FeedbackPromptResponses._ID, Tables.PROMPTS)
					.mapToTable(FeedbackPromptResponses.RESPONSE_ID, Tables.PROMPTS)
					.where(FeedbackResponses.CAMPAIGN_URN + "=?", campaignUrn)
					.where(FeedbackResponses.SURVEY_ID + "=?", surveyID)
					.where(Tables.PROMPTS + "." + FeedbackPromptResponses.PROMPT_ID + "=?", promptID);
				
			case MatcherTypes.CAMPAIGN_SURVEY_RESPONSES_PROMPTS_BY_ID_AGGREGATE:
				campaignUrn = uri.getPathSegments().get(0);
				surveyID = uri.getPathSegments().get(1);
				promptID = uri.getPathSegments().get(4);
				String aggregate = uri.getPathSegments().get(5);
				
				String toClause;
				
				switch (AggregateTypes.valueOf(aggregate)) {
					case AVG: toClause = "avg(" + FeedbackPromptResponses.PROMPT_VALUE + ")"; break;
					case COUNT: toClause = "count(" + FeedbackPromptResponses.PROMPT_VALUE + ")"; break;
					case MAX: toClause = "max(" + FeedbackPromptResponses.PROMPT_VALUE + ")"; break;
					case MIN: toClause = "min(" + FeedbackPromptResponses.PROMPT_VALUE + ")"; break;
					case TOTAL: toClause = "total(" + FeedbackPromptResponses.PROMPT_VALUE + ")"; break;
					default:
						throw new IllegalArgumentException("Specified aggregate was not one of AggregateTypes");
				}
				
				return builder.table(Tables.PROMPTS_JOIN_RESPONSES)
					.mapToTable(FeedbackPromptResponses._ID, Tables.PROMPTS)
					.mapToTable(FeedbackPromptResponses.RESPONSE_ID, Tables.PROMPTS)
					.map("aggregate", toClause)
					.where(FeedbackResponses.CAMPAIGN_URN + "=?", campaignUrn)
					.where(FeedbackResponses.SURVEY_ID + "=?", surveyID)
					.where(Tables.PROMPTS + "." + FeedbackPromptResponses.PROMPT_ID + "=?", promptID);
				
			case MatcherTypes.PROMPTS:
				return builder.table(Tables.PROMPTS);
				
			case MatcherTypes.PROMPT_BY_PID:
				promptID = uri.getPathSegments().get(1);
				
				return builder.table(Tables.PROMPTS)
					.where(FeedbackPromptResponses._ID + "=?", promptID);
				
			default:
				throw new UnsupportedOperationException("buildSelection(): Unknown URI: " + uri);
		}
	}
}
