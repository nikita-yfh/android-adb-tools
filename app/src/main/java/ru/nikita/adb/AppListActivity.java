package ru.nikita.adb;

import java.util.List;
import java.util.Iterator;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.graphics.drawable.Drawable;


public class AppListActivity extends ListActivity{
	private void updateList(){
		MenuItem item = menu.findItem(R.id.menu_installed_apps);
		boolean installed = item.isChecked();
		new AppLoader().execute(installed);
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		new AppLoader().execute(true);
    }
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		super.onListItemClick(l,v,position,id);
		Intent intent = new Intent();
		intent.setData(Uri.parse(apps[position].path));
		setResult(RESULT_OK, intent);
		finish();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.app_list_activity,menu);
		this.menu=menu;
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.menu_installed_apps:
			item.setChecked(!item.isChecked());
			updateList();
			return true;
		case R.id.menu_update_app_list:
			updateList();
			return true;
		}
		return false;
	}


	private class App {
		App(String name, String pkg, String path, Drawable icon){
			this.name=name;
			this.pkg=pkg;
			this.path=path;
			this.icon=icon;
		}
		public String name;
		public String pkg;
		public String path;
		public Drawable icon;
	}

	private class AppLoader extends AsyncTask<Boolean,Void,App[]>{
		@Override
		protected void onPreExecute(){
			pd = new ProgressDialog(AppListActivity.this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setMessage(getResources().getString(R.string.app_loading));
			pd.show();
		}
		@Override
		protected void onPostExecute(App[] apps){
			setListAdapter(new AppListAdapter(AppListActivity.this, apps));
			pd.dismiss();
		}
		@Override
		protected App[] doInBackground(Boolean ... installed){
			PackageManager pm = getPackageManager();
			List<ApplicationInfo> packages = pm.getInstalledApplications(0);

			if(installed[0]){
				Iterator<ApplicationInfo> it = packages.iterator();
				while(it.hasNext()){
					ApplicationInfo info = it.next();
					if((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
						it.remove();
				}
			}

			apps = new App[packages.size()];
			for(int i = 0; i < packages.size(); i++){
				ApplicationInfo info = packages.get(i);
				apps[i] = new App(
					pm.getApplicationLabel(info).toString(),
					info.packageName,
					info.sourceDir,
					pm.getApplicationIcon(info)
				);
			}
			return apps;
		}
		private ProgressDialog pd;
	}
	
	private class AppListAdapter extends ArrayAdapter<App>{
		AppListAdapter(Context context, App[] apps){
			super(context,R.layout.app_list_item,apps);
		}
		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			if(view == null){
				view = getLayoutInflater().inflate(R.layout.app_list_item, viewGroup, false);

				ViewHolder viewHolder = new ViewHolder();
				viewHolder.iconView = (ImageView) view.findViewById(R.id.app_icon);
				viewHolder.nameView = (TextView) view.findViewById(R.id.app_name);
				viewHolder.pkgView =  (TextView) view.findViewById(R.id.app_package);

				view.setTag(viewHolder);
			}
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			App app = getItem(i);
			viewHolder.nameView.setText(app.name);
			viewHolder.pkgView.setText(app.pkg);
			viewHolder.iconView.setImageDrawable(app.icon);
			return view;
		}
		private class ViewHolder{
			public ImageView iconView;
			public TextView nameView;
			public TextView pkgView;
		}
	}

	private App[] apps;
	private Menu menu;
}


