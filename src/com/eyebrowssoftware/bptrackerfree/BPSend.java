package com.eyebrowssoftware.bptrackerfree;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.eyebrowssoftware.bptrackerfree.BPRecords.BPRecord;

public class BPSend extends Activity implements CompoundButton.OnCheckedChangeListener, OnClickListener {

	private static final String TAG = "BPSend";

	private static final String[] PROJECTION = { BPRecord._ID,
			BPRecord.SYSTOLIC, BPRecord.DIASTOLIC, BPRecord.PULSE,
			BPRecord.CREATED_DATE };

	private static final int COLUMN_ID_INDEX = 0;
	private static final int COLUMN_SYSTOLIC_INDEX = 1;
	private static final int COLUMN_DIASTOLIC_INDEX = 2;
	private static final int COLUMN_PULSE_INDEX = 3;
	private static final int COLUMN_CREATED_AT_INDEX = 4;


	private static final int RECORDS_QUERY = 0; // for the async query handler

	private Uri mUri;
	private Cursor mRecordsCursor;

	private TextView mMsgLabelView;
	private TextView mMsgView;
	private CheckBox mSendText;
	private CheckBox mSendFile;
	private Button mSendButton;

	private String mMsgLabelString;

	public static final boolean ALL_DATES = true;
	public static final String REVERSE = "reverse";

	// This may or may not be used
	private boolean mReverse = true;

	MyQueryHandler mMQH;

	private String mMessage; // this is the message we construct to send

	private Calendar mToCalendar = GregorianCalendar.getInstance();
	private Calendar mFromCalendar = GregorianCalendar.getInstance();
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		this.setContentView(R.layout.bp_send);

		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(BPRecords.CONTENT_URI);
		}
		mReverse = intent.getBooleanExtra(REVERSE, true);

		mUri = intent.getData();
		
		mMsgLabelView = (TextView) findViewById(R.id.message_label);
		mMsgLabelString = getString(R.string.label_message_format);
		mMsgView = (TextView) findViewById(R.id.message);

		mSendText = (CheckBox) findViewById(R.id.text);
		mSendText.setChecked(true);
		mSendText.setOnCheckedChangeListener(this);

		mSendFile = (CheckBox) findViewById(R.id.attach);
		mSendFile.setChecked(true);
		mSendFile.setOnCheckedChangeListener(this);

		mSendButton = (Button) findViewById(R.id.send);
		mSendButton.setOnClickListener(this);

		mMQH = new MyQueryHandler();

		List<String> segments = mUri.getPathSegments();
		if(segments.size() > 1) { // this is a single item query
		}
		querySendData();
	}

	private class MyQueryHandler extends AsyncQueryHandler {

		public MyQueryHandler() {
			super(BPSend.this.getContentResolver());
		}

		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			BPSend.this.startManagingCursor(cursor);
			switch (token) {
			case RECORDS_QUERY:
				if (cursor != null && cursor.moveToFirst()) {
					long mNewest;
					long mOldest;
					long first_time = cursor.getLong(COLUMN_CREATED_AT_INDEX);
					cursor.moveToLast();
					long last_time = cursor.getLong(COLUMN_CREATED_AT_INDEX);
					if (mReverse) {
						mNewest = first_time;
						mOldest = last_time;
					} else {
						mOldest = first_time;
						mNewest = last_time;
					}
					mToCalendar.setTimeInMillis(mNewest);
					mFromCalendar.setTimeInMillis(mOldest);
				} else {
					long now = System.currentTimeMillis();
					mToCalendar.setTimeInMillis(now);
					mFromCalendar.setTimeInMillis(now);
					Toast.makeText(BPSend.this,
									R.string.msg_nothing_to_send,
									Toast.LENGTH_LONG).show();
				}
				mRecordsCursor = cursor;
				new UpdateMessageTask().execute();
				break;
			}
		}
	}

	private void querySendData() {
		String where = null;
		String[] whereArgs = null;

		mMQH.startQuery(RECORDS_QUERY, TAG, mUri, PROJECTION, where, whereArgs,
				BPRecord.CREATED_DATE + ((mReverse) ? " DESC" : "ASC"));
	}

	public void onClick(View v) {
		if (v.equals(mSendButton)) {
			if (mMessage != null) {
				sendData(mMessage);
				finish();
			} else {
				Toast.makeText(this, R.string.msg_nothing_to_send,
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void sendData(String value) {
		// We're going to send the data in two forms, as message text and as an
		// attachment
		FileOutputStream fos;
		if (value == null || !(mSendText.isChecked() || mSendFile.isChecked())) {
			Toast.makeText(this, R.string.msg_nothing_to_send,
					Toast.LENGTH_LONG).show();
			return;
		}
		try {
			fos = this.openFileOutput("data.csv", Context.MODE_WORLD_READABLE);
			fos.write(value.getBytes());
			fos.close();
			Uri fileUri = Uri.fromFile(getFileStreamPath("data.csv"));
			// Log.d(TAG, "File Uri: " + fileUri.toString());
			Intent i = new Intent(Intent.ACTION_SEND);
			i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			if (mSendText.isChecked())
				i.putExtra(Intent.EXTRA_TEXT, value);
			if (mSendFile.isChecked())
				i.putExtra(Intent.EXTRA_STREAM, fileUri);
			i.putExtra(Intent.EXTRA_TITLE, "bpdata.csv");
			i.putExtra(Intent.EXTRA_SUBJECT, "bpdata.csv");
			i.setType("text/plain");
			Intent ai = Intent.createChooser(i,
					getString(R.string.msg_choose_send_method));
			ai.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(ai);
		} catch (FileNotFoundException e) {
			Log.e(TAG, getString(R.string.title_error));
			Toast.makeText(this, getString(R.string.title_error), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, getString(R.string.title_error));
			Toast.makeText(this, getString(R.string.title_error), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}


	public void onCheckedChanged(CompoundButton check_box, boolean checked) {
		if (check_box.equals(mSendText) && !checked && !mSendFile.isChecked())
			Toast.makeText(BPSend.this, R.string.msg_nothing_to_send, Toast.LENGTH_SHORT).show();
		
		if (check_box.equals(mSendFile) && !checked && !mSendText.isChecked())
			Toast.makeText(BPSend.this, R.string.msg_nothing_to_send, Toast.LENGTH_SHORT).show();
	}


	// Uses the member Cursor mRecordsCursor
	private class UpdateMessageTask extends AsyncTask<Void, Integer, String> {
		
		String date_localized;
		String time_localized;
		String sys_localized;
		String dia_localized;
		String pls_localized;
		String delimiter;

		@Override
		protected void onPreExecute() {
			Resources res = getResources();
			date_localized = res.getString(R.string.bp_send_date);
			time_localized = res.getString(R.string.bp_send_time);
			sys_localized = res.getString(R.string.bp_send_sys);
			dia_localized = res.getString(R.string.bp_send_dia);
			pls_localized = res.getString(R.string.bp_send_pls);
			delimiter = res.getString(R.string.bp_send_delimiter);
		}

		@Override
		protected String doInBackground(Void... nada) {
			
			StringBuilder sb = new StringBuilder();
			Cursor cursor = mRecordsCursor;
			
			if (cursor != null && cursor.moveToFirst()) {
				
				String[] cnames = cursor.getColumnNames();
				int columns = cnames.length;
				
				for (int j = 0; j < columns; ++j) {
					if (j == COLUMN_ID_INDEX) { // we'll use this first column to put the overall status
						continue;
					}
					else if (j == COLUMN_SYSTOLIC_INDEX) {
						// add two columns, for systolic_red and systolic_orange
						sb.append(sys_localized);
					} else if (j == COLUMN_DIASTOLIC_INDEX) {
						// add two columns, for diastolic_red and
						// diastolic_orange
						sb.append(dia_localized);
					} else if (j == COLUMN_PULSE_INDEX) {
						// add two columns, for pulse_red and pulse_orange
						sb.append(pls_localized);
					} else if (j == COLUMN_CREATED_AT_INDEX) {
						// This turns into two columns
						sb.append(date_localized).append(delimiter).append(time_localized);
					} else
						sb.append(cnames[j]);
					sb.append((j + 1 == columns) ? '\n' : delimiter);
				}
				do {
					// the final separator of each field is put on at the end.
					for (int j = 0; j < columns; ++j) {
						if (j == COLUMN_ID_INDEX) {
							continue;
						} else if (j == COLUMN_SYSTOLIC_INDEX) {
							int value = cursor.getInt(j);
							sb.append(String.valueOf(value));
						} else if (j == COLUMN_DIASTOLIC_INDEX) {
							int value = cursor.getInt(j);
							sb.append(String.valueOf(value));
						} else if (j == COLUMN_PULSE_INDEX) {
							int value = cursor.getInt(j);
							sb.append(String.valueOf(value));
						} else if (j == COLUMN_CREATED_AT_INDEX) {
							String date = BPTracker.getDateString(cursor
									.getLong(j), DateFormat.SHORT);
							String time = BPTracker.getTimeString(cursor
									.getLong(j), DateFormat.SHORT);
							sb.append(date).append(delimiter).append(time);
						} else
							sb.append(String.valueOf(cursor.getInt(j)));
						sb.append((j + 1 == columns) ? '\n' : delimiter);
					}
				} while (cursor.moveToNext());
			}
			return sb.toString();
		}

		@Override
		protected void onPostExecute(String message) {
			mMessage = message;
			mMsgLabelView.setText(String.format(mMsgLabelString, message
					.length()));
			mMsgView.setText(message);
		}
	}
	
}
