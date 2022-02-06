package ru.nikita.adb;

import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Comparable;
import android.os.Bundle;
import android.os.AsyncTask;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Toast;
import android.R.layout;
import android.util.SparseBooleanArray;
import ru.nikita.adb.Device;
import ru.nikita.adb.Task;

public class PermissionsActivity extends ListActivity{
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		adb = (Binary) intent.getSerializableExtra("adb");
		device = (Device) intent.getSerializableExtra("device");
		pkg = intent.getStringExtra("package");
		new AppPermissionLoadTask().execute();
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.permissions_activity, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id = item.getItemId();
		if(id == R.id.permissions_apply){
			SparseBooleanArray selected = getListView().getCheckedItemPositions();
			boolean granted[] = new boolean[permissions.length];
			for(int i = 0; i < granted.length; i++)
				granted[i] = selected.get(i);
			new AppPermissionsApplyTask().execute(granted);
			return true;
		}
		return false;
	}
	private class Permission implements Comparable<Permission>{
		Permission(String name){
			this.name = name;
			granted = false;
		}
		@Override 
		public int compareTo(Permission p){
			return this.name.compareTo(p.name);
		}
		@Override
		public String toString(){
			return name;
		}
		public String name;
		public boolean granted;
	}
	private class AppPermissionLoadTask extends ADBTask{
		AppPermissionLoadTask(){
			super(adb);
		}
		@Override
		protected void onPreExecute(){
			super.onPreExecute();
			pd = new ProgressDialog(PermissionsActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setMessage(getResources().getString(R.string.permissions_loading));
			pd.show();
		}
		@Override
		protected void onPostExecute(String log){
			super.onPostExecute(log);
			pd.dismiss();
			String[] lines = log.split("\n");
			boolean requested = false;
			boolean install = false;
			ArrayList<Permission>list = new ArrayList<Permission>();
			for(String line : lines){
				line = line.trim();
				if(line.equals("requested permissions:"))
					requested = true;
				else if(line.equals("install permissions:"))
					install = true;
				else if(line.contains("User"))
					break;
				else if(install){
					if(!line.contains("REVOKE_ON_UPGRADE")){
						String name = line.split(":")[0];
						for(Permission p : list)
							if(p.name.equals(name))
								p.granted = true;
					}
				}else if(requested)
					list.add(new Permission(line));
			}
			permissions = list.toArray(new Permission[0]);
			Arrays.sort(permissions);

			setListAdapter(new ArrayAdapter<Permission>(PermissionsActivity.this, 
					android.R.layout.simple_list_item_multiple_choice, permissions));

			ListView listView = getListView();
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			for(int i = 0; i < permissions.length; i++)
				listView.setItemChecked(i, permissions[i].granted);
		}

		public void execute(){
			shell(device, "dumpsys package " + pkg);
		}
		private ProgressDialog pd;
	}
	private class AppPermissionsApplyTask extends ADBTask{
		AppPermissionsApplyTask(){
			super(adb);
		}
		@Override
		protected void onPreExecute(){
			super.onPreExecute();
			pd = new ProgressDialog(PermissionsActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setMessage(getResources().getString(R.string.app_permissions_applying));
			pd.show();
		}
		@Override
		protected void onPostExecute(String log){
			super.onPostExecute(log);
			pd.dismiss();
			if(log.length() > 0)
				Toast.makeText(getApplicationContext(), log, Toast.LENGTH_LONG).show();
			new AppPermissionLoadTask().execute();
		}
		public void execute(boolean[] granted){
			String command = "";
			for(int i = 0; i < permissions.length; i++){
				Permission permission = permissions[i];
				if(permission.granted != granted[i])
					command += String.format("pm %s %s %s; ", granted[i]?"grant":"revoke",
							pkg, permission.name);
			}
			shell(device, command);
		}
		private ProgressDialog pd;
	}


	private Binary adb;
	private Device device;
	private String pkg;

	private Permission[] permissions;
}
