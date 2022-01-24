package ru.nikita.adb;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.content.Intent;
import ru.nikita.adb.ADBActivity;
import ru.nikita.adb.FastbootActivity;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
	}

	public void onADBClicked(View view){
		Intent intent = new Intent(this, ADBActivity.class);
		startActivity(intent);
	}
	public void onFastbootClicked(View view){
		Intent intent = new Intent(this, FastbootActivity.class);
		startActivity(intent);
	}
}
