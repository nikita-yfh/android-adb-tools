package ru.nikita.adb;
import java.io.InputStream;
import java.io.IOException;
import android.content.res.AssetManager;
import android.content.Context;
import android.widget.TextView;
import android.os.AsyncTask;
import java.io.InputStreamReader;
import ru.nikita.adb.Binary;

public class Task extends AsyncTask<String, String, String> {
	public class ProcessErrorException extends Exception{
		ProcessErrorException(String msg){
			super(msg);
		}
	}
	public Task(TextView text, Binary binary){
		this.text=text;
		this.binary=binary;
	}
	@Override
	protected void onPreExecute(){
		if(text != null)
			text.setText("");
	}
	@Override
	protected void onProgressUpdate(String ... values) {
		super.onProgressUpdate(values);
		if(text != null)
			text.setText(values[0]);
	}
	protected String executeNow(String args) throws ProcessErrorException{
		String out = "";
		String err = "";
		try{
			ProcessBuilder processBuilder = new ProcessBuilder(binary.getCmd(args));
            Process process = processBuilder.start();
			InputStreamReader stdout = new InputStreamReader(process.getInputStream());
			InputStreamReader stderr = new InputStreamReader(process.getErrorStream());

			while(true){
				String _out = getText(stdout);
				String _err = getText(stderr);
				if(_out == null && _err == null)
					break;
				if(_out != null)
					out += _out;
				if(_err != null)
					err += _err;
			}
			process.waitFor();
			process.destroy();

        }catch(Exception e) {
            e.printStackTrace();
        }
		if(!err.isEmpty())
			throw new ProcessErrorException(err);
		return out;
	}

	@Override
	protected String doInBackground(String... strings) {
		String log = "";
        try {
			String command = "> "+binary.getReadableCmd(strings[0]);
			publishProgress(command);

			ProcessBuilder processBuilder = new ProcessBuilder(binary.getCmd(strings[0]));
			processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
			InputStreamReader reader = new InputStreamReader(process.getInputStream());

			while(true){
				String str=getText(reader);
				if(str == null)
					break;
				log += str;
				publishProgress(String.format("%s\n\n%s",command,log));
			}


			process.waitFor();
			process.destroy();

			publishProgress(String.format("%s\n\n%s\nProcess returned %d",command,log,process.exitValue()));

        }catch(Exception e) {
            e.printStackTrace();
        }
		return log;
	}

	private String getText(InputStreamReader reader){
		try{
			final char[] buf = new char[256];
			final int read = reader.read(buf);
			if (read < 1) 
				return null;
			String str = new String(buf).replaceAll("\t", " ");
			return str;
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

	protected TextView text;
	protected Binary binary;
}
