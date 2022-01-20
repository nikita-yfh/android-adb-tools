package ru.nikita.adb;
import java.io.InputStream;
import java.io.IOException;
import android.content.res.AssetManager;
import android.content.Context;
import android.widget.TextView;
import android.os.AsyncTask;
import java.io.InputStreamReader;
import ru.nikita.adb.Binary;

public class Task extends AsyncTask<String, Void, String> {
	public Task(TextView text, Binary binary){
		this.text=text;
		this.binary=binary;
	}
	@Override
	protected void onPreExecute(){
		text.setText("");
	}
	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
		text.setText(log);
	}

	@Override
	protected String doInBackground(String... strings) {
        try {
			log = "> "+binary.getReadableCmd(strings[0])+"\n\n";
			publishProgress();

			ProcessBuilder processBuilder = new ProcessBuilder(binary.getCmd(strings[0]));
			processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
			InputStreamReader reader = new InputStreamReader(process.getInputStream());

			while(appendText(reader));

			process.waitFor();
			process.destroy();

			log += String.format("\nProcess returned code %d",process.exitValue());
			publishProgress();

        }catch(Exception e) {
            e.printStackTrace();
        }
		return log;
	}

	private boolean appendText(InputStreamReader reader){
		try{
			final char[] buf = new char[256];
			final int read = reader.read(buf);
			if (read < 1) 
				return false;
			String str = new String(buf).replaceAll("\t", " ");
			log += str;
			publishProgress();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}

	protected TextView text;
	protected Binary binary;
	protected String log;
}
