package ru.nikita.adb;

import android.os.Bundle;
import android.os.AsyncTask;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import ru.nikita.adb.Device;
import ru.nikita.adb.Task;

public class AppManagerActivity extends ListActivity{
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		adb = (Binary) getIntent().getSerializableExtra("adb");
		device = (Device) getIntent().getSerializableExtra("device");

    }
	
	private Device device;
	private Binary adb;
}

