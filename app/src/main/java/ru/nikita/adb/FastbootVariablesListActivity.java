package ru.nikita.adb;

import android.os.Bundle;
import android.os.AsyncTask;
import android.app.ListActivity;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import ru.nikita.adb.FastbootVariable;

public class FastbootVariablesListActivity extends ListActivity{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		variables = (FastbootVariable[]) getIntent().getSerializableExtra("variables");
		setListAdapter(new VariablesListAdapter(this, variables));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id){
		FastbootVariable variable = variables[position];
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText(variable.name, variable.value);
		clipboard.setPrimaryClip(clip);

		Toast.makeText(this, getResources().getString(R.string.clipboard_copied), Toast.LENGTH_SHORT).show();
	}

	private class VariablesListAdapter extends ArrayAdapter<FastbootVariable>{
		VariablesListAdapter(Context context, FastbootVariable[] apps){
			super(context, R.layout.variable_list_item, apps);
		}
		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			if(view == null){
				view = getLayoutInflater().inflate(R.layout.variable_list_item, viewGroup, false);

				ViewHolder viewHolder = new ViewHolder();
				viewHolder.nameView = (TextView) view.findViewById(R.id.variable_name);
				viewHolder.valueView =  (TextView) view.findViewById(R.id.variable_value);

				view.setTag(viewHolder);
			}
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			FastbootVariable variable = getItem(i);
			viewHolder.nameView.setText(variable.name);
			viewHolder.valueView.setText(variable.value);
			return view;
		}
		private class ViewHolder{
			public TextView nameView;
			public TextView valueView;
		}
	}
	private FastbootVariable[] variables;
}

