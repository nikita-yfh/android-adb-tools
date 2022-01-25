package ru.nikita.adb;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import android.os.Bundle;
import android.app.ListActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.view.View;
import android.view.Menu;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.WindowManager;
import android.content.Intent;
import android.text.InputType;
import ru.nikita.adb.Task;
import ru.nikita.adb.Binary;
import ru.nikita.adb.ADBFile;
import ru.nikita.adb.ADBActivity;

public class FileManagerActivity extends ListActivity{
	private static final int FILE_PUSH=1;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		adb = new Binary(getApplicationContext(), intent.getStringExtra("adb"));
		device = new Device(intent.getStringExtra("device"));
		ListView lv = getListView();
		registerForContextMenu(lv);
		history = new ArrayList<ADBFile>();
		updateFileList(new ADBFile(adb, device, "/sdcard"), false);
	}
	private void updateFileList(ADBFile file, boolean saveHistory){
		try{
			if(saveHistory)
				history.add(currentFile);
			currentFile = file;
			fileList = file.listFiles();
			setListAdapter(new FileListAdapter(this, fileList));
			setTitle(file.getPath());
		}catch(Exception e){
			makeToast(e.toString());
		}
	}
	private void makeToast(String text){
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && data != null){
			if(requestCode == FILE_PUSH){
				String filePath = data.getData().getPath();
				new ADBTask(adb){
					@Override
					protected void onPostExecute(String log){
						updateFileList(currentFile, false);
					}
				}.push(device, filePath, currentFile.getPath());
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.file_manager_activity,menu);
		return true;
	}
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.file_context_menu, menu);
    }
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id = item.getItemId();
		if(id == R.id.refresh_files){
			updateFileList(currentFile, false);
			return true;
		}else if(id == R.id.file_push){
			Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
			chooseFileIntent.setType("*/*");
			chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

			chooseFileIntent = Intent.createChooser(chooseFileIntent, getResources().getString(R.string.file_choose));
			startActivityForResult(chooseFileIntent, FILE_PUSH);
			return true;
		}
		return false;
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		super.onListItemClick(l,v,position,id);
		ADBFile selected = fileList[position];
		if(selected.isDirectory())
			updateFileList(selected, true);
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int id = item.getItemId();
		if(id == R.id.file_delete){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.file_delete_confirm);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					try{
						fileList[info.position].delete();
						updateFileList(currentFile, false);
					}catch(Exception e){
						makeToast(e.toString());
					}
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			builder.show();
			return true;
		}else if(id == R.id.file_rename){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.file_rename);

			final EditText input = new EditText(this);
			input.setText(fileList[info.position].getName());
			input.setSelectAllOnFocus(true);
			builder.setView(input);

			builder.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String text=input.getText().toString();
					fileList[info.position].rename(text);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			builder.show();
			return true;
		}else if(id == R.id.file_pull){
			File dir = new File("/sdcard/ADB");
			dir.mkdir();
			fileList[info.position].pull(dir.getPath());
			return true;
		}

		return super.onContextItemSelected(item);
	}
	@Override
	public void onBackPressed(){
		if(history.isEmpty())
			finish();
		else{
			ADBFile last = history.get(history.size() - 1);
			updateFileList(last, false);
			history.remove(last);
		}
	}
	private class FileListAdapter extends ArrayAdapter<ADBFile>{
		public FileListAdapter(Context context, ADBFile[] files){
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
			ADBFile file = getItem(i);

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
	private ADBFile currentFile;
	private ADBFile[] fileList;
	private List<ADBFile> history;
	private Device device;
	private Binary adb;
}


