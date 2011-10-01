package com.eyebrowssoftware.bptrackerfree;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.eyebrowssoftware.bptrackerfree.BPRecords.BPRecord;

public class BPRecordEditor extends Activity {

	// Static constants
	
	private static final String TAG = "BPRecordEditor";

	// Identifiers of our menu items
	private static final int DONE_ID = Menu.FIRST;
	private static final int REVERT_ID = Menu.FIRST + 1;
	private static final int DISCARD_ID = Menu.FIRST + 2;
	private static final int DELETE_ID = Menu.FIRST + 3;
	
	// The different distinct states the activity can be run in.
	private static final int STATE_EDIT = 0;
	private static final int STATE_INSERT = 1;

	private static final int DELETE_DIALOG_ID = 0;
	
	// The menu group, for grouped items
	private static final int MENU_GROUP = Menu.NONE + 1;
	
	private int mState = 0; // XXX: Dubious temporary

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		final Intent intent = getIntent();
		final String action = intent.getAction();

		setContentView(R.layout.bp_record_editor);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Build the menus that are shown when editing.
		if (mState == STATE_EDIT) {
			menu.add(MENU_GROUP, DONE_ID, 0, R.string.menu_done);
			menu.add(MENU_GROUP, REVERT_ID, 1, R.string.menu_revert);
			menu.add(MENU_GROUP, DELETE_ID, 2, R.string.menu_delete);
		} else {
			menu.add(MENU_GROUP, DONE_ID, 0, R.string.menu_done);
			menu.add(MENU_GROUP, DISCARD_ID, 1, R.string.menu_discard);
		}
		return true;
	}
	
	@Override
	protected void onResume() {
		// Modify our overall title depending on the mode we are running in.
		if (mState == STATE_EDIT) {
			setTitle(getText(R.string.title_edit));
		} else if (mState == STATE_INSERT) {
			setTitle(getText(R.string.title_create));
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DELETE_DIALOG_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.really_delete))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.label_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						//TODO: deleteRecord();
						setResult(RESULT_OK);
						finish();
					}
				})
				.setNegativeButton(getString(R.string.label_no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
			return builder.create();
		default:
			return null;
		}
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case DELETE_ID:
			showDialog(DELETE_DIALOG_ID);
			return true;
		case DISCARD_ID:
			// TODO: cancelRecord();
			return true;
		case REVERT_ID:
			// TODO: cancelRecord();
			return true;
		case DONE_ID:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
}
