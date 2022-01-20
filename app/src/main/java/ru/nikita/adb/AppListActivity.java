package ru.nikita.adb;

import java.util.List;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class AppListActivity extends ListActivity{
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		PackageManager pm = getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		App[] apps = new App[packages.size()];
		for(int i = 0; i < packages.size(); i++){
			ApplicationInfo info = packages.get(i);
			apps[i] = new App(
				pm.getApplicationLabel(info).toString(),
				info.packageName,
				"",
				pm.getApplicationIcon(info)
			);
		}

		setListAdapter(new AppListAdapter(this,apps));
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
}


