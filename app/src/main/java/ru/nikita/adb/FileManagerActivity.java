package ru.nikita.adb;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import android.os.Bundle;
import android.net.Uri;
import android.app.ListActivity;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Intent;

public class FileManagerActivity extends ListActivity{
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		history = new ArrayList<File>();
		updateFileList(new File("/sdcard"), false);
	}
	private void updateFileList(File file, boolean saveHistory){
		if(saveHistory)
			history.add(currentFile);
		currentFile = file;
		fileList = file.listFiles();
		Arrays.sort(fileList, new FileComparator());
		setListAdapter(new FileListAdapter(this, fileList));
		setTitle(file.getPath());
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		super.onListItemClick(l,v,position,id);
		File selected = fileList[position];
		if(selected.isDirectory())
			updateFileList(selected, true);
		else{
			Intent intent = new Intent();
			intent.setData(Uri.parse(selected.getPath()));
			setResult(RESULT_OK, intent);
			finish();
		}

	}
	@Override
	public void onBackPressed(){
		if(history.isEmpty()){
			setResult(RESULT_CANCELED);
			finish();
		}else{
			File last = history.get(history.size() - 1);
			updateFileList(last, false);
			history.remove(last);
		}
	}
	private class FileComparator implements Comparator<File>{
		public int compare(File a, File b){
			return a.getName().compareTo(b.getName());
		}
	}

	private class FileListAdapter extends ArrayAdapter<File>{
		public FileListAdapter(Context context, File[] files){
			super(context,R.layout.file_list_item,files);
		}
		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			if(view == null){
				view = getLayoutInflater().inflate(R.layout.file_list_item, viewGroup, false);

				ViewHolder viewHolder = new ViewHolder();
				viewHolder.iconView = (ImageView) view.findViewById(R.id.file_icon);
				viewHolder.nameView = (TextView) view.findViewById(R.id.file_name);

				view.setTag(viewHolder);
			}
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			File file = getItem(i);

			viewHolder.nameView.setText(file.getName());
			int icon = file.isDirectory() ? R.drawable.folder : R.drawable.document;
			viewHolder.iconView.setImageResource(icon);
			return view;
		}
		private class ViewHolder{
			public ImageView iconView;
			public TextView nameView;
		};
	}
	private File currentFile;
	private File[] fileList;
	private List<File> history;
}

