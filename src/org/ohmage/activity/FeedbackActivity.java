
package org.ohmage.activity;

import org.ohmage.ChartFragment;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.SparkLine;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.ui.BaseActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Calendar;

public class FeedbackActivity extends BaseActivity {

	public static final double[] FAKE_DATA1 = new double[] {
			1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0
	};

	public static final double[] FAKE_DATA2 = new double[] {
			1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 5, 5, 5, 5, 5, 5
	};

	private static final String TAG = "FeedbackActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_layout);

		if (getSupportFragmentManager().findFragmentById(R.id.chart1) == null) {
			setChart("Exercise Frequency", FAKE_DATA1, R.id.chart1_title, R.id.chart1, R.color.powderkegblue, R.color.light_blue);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.chart2) == null) {
			setChart("Healthy Diet", FAKE_DATA2, R.id.chart2_title, R.id.chart2, R.color.dark_green, R.color.light_green);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.chart3) == null) {
			setChart("Random Graph", Utilities.randomData(30, 9), R.id.chart3_title, R.id.chart3, R.color.dark_purple, R.color.light_purple);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_response_graph) == null) {
			ChartFragment f = ChartFragment.newInstance(buildResponseFrequencyGraph());
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_response_graph, f)
					.commit();
		}
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		Button b = (Button) findViewById(R.id.feedback_charts_more);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(FeedbackActivity.this, ChartFeedbackActivity.class));
			}
		});
		b = (Button) findViewById(R.id.feedback_response_history_more);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(FeedbackActivity.this, ResponseHistoryActivity.class));
			}
		});
	}

	private void setChart(String title, double[] data, int chartTitleId, int chartId, int colorId, int fillColorId) {
		SparkLine chart = new SparkLine(this, data, getResources().getColor(colorId), getResources().getColor(fillColorId));
		chart.getRenderer().setInScroll(true);
		((TextView) findViewById(chartTitleId)).setText(title);
		ChartFragment f = ChartFragment.newInstance(chart);
		getSupportFragmentManager().beginTransaction().add(chartId, f).commit();
	}

	private Histogram buildResponseFrequencyGraph() {

		double[] values = new double[10];
		Arrays.fill(values, 0);

		// Queries for the responses which were taken in the last 10 days
		// Some extra responses may slip into this query for exampl if we are querying at 1pm and they happened
		// after 1pm on the day before the last day we are including. However these responses will not be included
		// in the histogram
		Cursor c = managedQuery(Responses.CONTENT_URI, new String[] { Responses.RESPONSE_TIME },
				Responses.RESPONSE_TIME + ">" + (Calendar.getInstance().getTimeInMillis() - values.length * DateUtils.DAY_IN_MILLIS), null,
				Responses.RESPONSE_TIME + " DESC");

		for(int i=0;i<values.length;i++) {
			while(c.moveToNext() && DateUtils.isToday(c.getLong(0) + DateUtils.DAY_IN_MILLIS * i)) {
				values[i]++;
			}
			c.moveToPrevious();
		}

		Histogram.HistogramRenderer renderer = new Histogram.HistogramRenderer(this);
		renderer.setMargins(new int[] {
				30, 35, 15, 30
		});
		renderer.setYLabels(5);
		renderer.setYTitle("# of Responses");
		renderer.setInScroll(true);

		return new Histogram(this, renderer, values);
	}
}
