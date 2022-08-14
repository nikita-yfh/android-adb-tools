package ru.nikita.adb;

import java.util.ArrayList;
import java.text.DecimalFormat;
import android.os.Bundle;
import android.os.AsyncTask;
import android.app.ListActivity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import ru.nikita.adb.FastbootVariable;

public class PartitionListActivity extends ListActivity{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FastbootVariable[] variables = (FastbootVariable[]) getIntent().getSerializableExtra("variables");

		ArrayList<Partition> partitions = new ArrayList<Partition>();

		FastbootVariable size = null;

		for(FastbootVariable variable : variables) {
			if(variable.name.startsWith("partition-size"))
				size = variable;
			else if(variable.name.startsWith("partition-type") && size != null)
				partitions.add(new Partition(variable, size));
		}
		this.partitions = partitions.toArray(new Partition[0]);

		setListAdapter(new PartitionListAdapter(this, this.partitions));
	}

	public static String readableFileSize(long size) {
		if(size <= 0) return "0";
		final char[] units = new char[] {' ', 'k', 'M', 'G', 'T'};
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return String.format("%.1f %cB (0x%x)", size / Math.pow(1024, digitGroups), units[digitGroups], size);
	}

	private class Partition {
		Partition(FastbootVariable type, FastbootVariable size) {
			this.name = type.name.substring(type.name.indexOf(':') + 1);
			this.type = type.value;
			long sizeBytes = Long.parseLong(size.value, 16);
			this.size = readableFileSize(sizeBytes);
		}

		String name;
		String type;
		String size;
	}

	private class PartitionListAdapter extends ArrayAdapter<Partition>{
		PartitionListAdapter(Context context, Partition[] partirions){
			super(context, R.layout.partition_list_item, partitions);
		}
		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			if(view == null){
				view = getLayoutInflater().inflate(R.layout.partition_list_item, viewGroup, false);

				ViewHolder viewHolder = new ViewHolder();
				viewHolder.nameView = (TextView) view.findViewById(R.id.partition_name);
				viewHolder.typeView = (TextView) view.findViewById(R.id.partition_type);
				viewHolder.sizeView = (TextView) view.findViewById(R.id.partition_size);

				view.setTag(viewHolder);
			}
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			Partition partition = (Partition) getItem(i);
			viewHolder.nameView.setText(partition.name);
			viewHolder.typeView.setText(partition.type);
			viewHolder.sizeView.setText(partition.size);
			return view;
		}
		private class ViewHolder{
			public TextView nameView;
			public TextView typeView;
			public TextView sizeView;
		}
	}
	private Partition[] partitions;
}

