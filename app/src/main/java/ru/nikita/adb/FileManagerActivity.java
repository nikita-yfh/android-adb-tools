package ru.nikita.adb;

import java.io.File;
import java.util.List;
import java.util.Iterator;
import android.os.Bundle;
import android.app.ListActivity;
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
import android.content.Intent;

public class FileManagerActivity extends ListActivity{
	FileManagerActivity(File currentFile){
		this.currentFile=currentFile;
	}
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}
//	private class FileListAdapter extends ArrayAdapter<File>{
//		public FileListAdapter(Context context, File file){
//			super.onCreate(context,R.layout.file_list_item,file.listFiles());
//		}
//
//	}
	private File currentFile;
}

