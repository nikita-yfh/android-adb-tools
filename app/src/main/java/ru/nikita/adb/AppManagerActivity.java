package ru.nikita.adb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import android.os.Bundle;
import android.os.AsyncTask;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.graphics.Color;
import android.util.Log;
import ru.nikita.adb.Device;
import ru.nikita.adb.Task;

public class AppManagerActivity extends ListActivity{
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		adb = (Binary) getIntent().getSerializableExtra("adb");
		device = (Device) getIntent().getSerializableExtra("device");
		ListView v = getListView();
		registerForContextMenu(v);
		new AppLoadTask().execute();
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.app_manager_activity, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id = item.getItemId();
		if(id == R.id.refresh_apps){
			new AppLoadTask().execute();
			return true;
		}
		return false;
	}
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.app_context_menu, menu);
    }
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int id = item.getItemId();
		App app = apps[info.position];
		if(id == R.id.app_uninstall_data){
			new AppTask().execute(R.string.app_uninstalling, "pm uninstall --user 0 " + app.pkg);
			return true;
		}else if(id == R.id.app_uninstall){
			new AppTask().execute(R.string.app_uninstalling, "pm uninstall -k --user 0 " + app.pkg);
			return true;
		}else if(id == R.id.app_install){
			new AppTask().execute(R.string.app_installing, "cmd package install-existing " + app.pkg);
			return true;
		}

		return super.onContextItemSelected(item);
	}

	private class App{
		App(String pkg, String path, boolean installed){
			this.pkg=pkg;
			this.path=path;
			this.installed=installed;
		}
		public String pkg;
		public String path;
		public boolean installed;
	}
	private class AppTask extends ADBTask{
		public AppTask(){
			super(adb);
		}
		@Override
		protected void onPreExecute(){
			super.onPreExecute();
			pd = new ProgressDialog(AppManagerActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setMessage(getResources().getString(stringId));
			pd.show();
		}
		@Override
		protected void onPostExecute(String log){
			pd.dismiss();
			log = log.trim();
			if(log.length() > 0 && log.length() < 200)
				Toast.makeText(getApplicationContext(), log, Toast.LENGTH_SHORT).show();
		}
		public void execute(int stringId, String args){
			this.stringId=stringId;
			shell(device, args);
		}

		int stringId;
	}
	private class AppComparator implements Comparator<App>{
		public int compare(App a, App b){
			return a.pkg.compareTo(b.pkg);
		}
	}
	private class AppLoadTask extends AppTask{
		@Override
		protected void onPostExecute(String log){
			super.onPostExecute(log);
			String lines[] = log.split("\\n");
			ArrayList<App> apps = new ArrayList<App>();

			Pattern pattern = Pattern.compile("package:(\\S+)=(\\S+)");
			for(String line : lines){
				if (line.matches(pattern.pattern())) {
					Matcher matcher = pattern.matcher(line);
					if(matcher.find()){
						App app = new App(matcher.group(2), matcher.group(1), false);
						boolean find = false;
						for(App i : apps)
							if(app.pkg.equals(i.pkg)){
								i.installed = true;
								find = true;
							}
						if(!find)
							apps.add(app);
					}
				}
			}
			AppManagerActivity.this.apps = apps.toArray(new App[0]);

			Arrays.sort(AppManagerActivity.this.apps, new AppComparator());

			setListAdapter(new AppListAdapter(AppManagerActivity.this, AppManagerActivity.this.apps));
		}
		public void execute(){
			execute(R.string.app_loading, "pm list packages -u -f; pm list packages -f");
		}
	}
	private class AppListAdapter extends ArrayAdapter<App>{
		AppListAdapter(Context context, App[] apps){
			super(context,R.layout.app_list_item,apps);
		}
		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			if(view == null){
				view = getLayoutInflater().inflate(R.layout.app_manager_list_item, viewGroup, false);

				ViewHolder viewHolder = new ViewHolder();
				viewHolder.pkgView =  (TextView) view.findViewById(R.id.app_package);
				viewHolder.pathView = (TextView) view.findViewById(R.id.app_path);

				view.setTag(viewHolder);
			}
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			App app = getItem(i);
			viewHolder.pkgView.setText(app.pkg);
			viewHolder.pathView.setText(app.path);

			if(!app.installed)
				view.setBackgroundColor(Color.argb(90, 255, 0, 0));
			else
				view.setBackgroundColor(Color.TRANSPARENT);
			return view;
		}
		private class ViewHolder{
			public TextView pkgView;
			public TextView pathView;
		}
	}
	private ProgressDialog pd;
	private App[] apps;
	
	private Device device;
	private Binary adb;
}

