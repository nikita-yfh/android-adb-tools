package ru.nikita.adb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Comparable;
import android.os.Bundle;
import android.os.AsyncTask;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import ru.nikita.adb.PermissionsActivity;

public class AppManagerActivity extends ListActivity{
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		sortMode = SORT.TYPE;
		Intent intent = getIntent();
		adb = (Binary) intent.getSerializableExtra("adb");
		device = (Device) intent.getSerializableExtra("device");
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
		}else if(id == R.id.sort_name){
			item.setChecked(true);
			sortMode = SORT.NAME;
			sortApps();
		}else if(id == R.id.sort_type){
			item.setChecked(true);
			sortMode = SORT.TYPE;
			sortApps();
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
			new AppTask().uninstallWithData(app);
			return true;
		}else if(id == R.id.app_uninstall){
			new AppTask().uninstall(app);
			return true;
		}else if(id == R.id.app_install){
			new AppTask().install(app);
			return true;
		}else if(id == R.id.app_clear){
			new AppTask().clear(app);
			return true;
		}else if(id == R.id.app_permissions){
			Intent intent = new Intent(this, PermissionsActivity.class);
			Bundle bundle = new Bundle();
			bundle.putSerializable("adb", adb);
			bundle.putSerializable("device", device);
			bundle.putString("package", app.pkg);
			intent.putExtras(bundle);
			startActivity(intent);
			return true;
		}
		return super.onContextItemSelected(item);
	}
	private void sortApps(){
		Arrays.sort(apps);
		setListAdapter(new AppListAdapter(this, apps));
	}
	private class App implements Comparable<App>{
		App(String pkg, String path){
			this.pkg=pkg;
			this.path=path;
			this.installed=false;
			this.system=true;
		}
		public int getColor(){
			if(!installed)
				return Color.argb(70, 255, 0, 0);
			if(!system)
				return Color.argb(70, 0, 255, 0);
			return Color.TRANSPARENT;
		}
		private boolean bigger(boolean a, boolean b){
			return a && !b;
		}
		@Override
		public int compareTo(App app){
			if(sortMode == SORT.TYPE){
				if(bigger(app.system, this.system) || bigger(this.installed, app.installed))
					return -1;
				if(bigger(this.system, app.system) || bigger(app.installed, this.installed))
					return 1;
			}
			return this.pkg.compareTo(app.pkg);
		}
		public String pkg;
		public String path;
		public boolean installed;
		public boolean system;
	}
	private class AppTask extends ADBTask{
		public AppTask(){
			super(adb);
			update = false;
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
			if(update)
				new AppLoadTask().execute();
		}
		public void execute(int stringId, String args){
			this.stringId=stringId;
			shell(device, args);
		}
		public void uninstallWithData(App app){
			update = true;
			execute(R.string.app_uninstalling, "pm uninstall --user 0 " + app.pkg);
		}
		public void uninstall(App app){
			update = true;
			execute(R.string.app_uninstalling, "pm uninstall -k --user 0 " + app.pkg);
		}
		public void install(App app){
			update = true;
			execute(R.string.app_installing, "cmd package install-existing " + app.pkg);
		}
		public void clear(App app){
			execute(R.string.app_clearing, "pm clear " + app.pkg);
		}

		int stringId;
		boolean update;
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
						App app = new App(matcher.group(2), matcher.group(1));
						boolean find = false;
						for(App i : apps)
							if(app.pkg.equals(i.pkg)){
								if(i.installed)
									i.system = false;
								i.installed = true;
								find = true;
							}
						if(!find)
							apps.add(app);
					}
				}
			}
			AppManagerActivity.this.apps = apps.toArray(new App[0]);
			sortApps();
		}
		public void execute(){
			execute(R.string.app_loading, "pm list packages -u -f; pm list packages -f; pm list packages -3 -f");
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

			view.setBackgroundColor(app.getColor());
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

	private enum SORT{
		TYPE,
		NAME
	}

	private SORT sortMode;
}

