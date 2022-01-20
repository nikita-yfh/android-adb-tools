package ru.nikita.adb;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.content.res.AssetManager;
import android.content.Context;


public class Binary{
	public Binary(Context context, String name){
		this.fileName=name;
		filesDir=context.getFilesDir().getAbsolutePath();
		String filePath = filesDir + "/" + name;
		File file = new File(filePath);
		if (!file.exists()){
			try{
				InputStream asset = context.getAssets().open(name);
				FileOutputStream output = new FileOutputStream(file);
				final byte[] buffer = new byte[1024];
				int size;
				while ((size = asset.read(buffer)) != -1)
					output.write(buffer, 0, size);
				asset.close();
				output.close();

				Runtime.getRuntime().exec("chmod 777 " + filePath);
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}
	protected String[] getCmd(String args) {
		String exec = String.format("HOME=%s TEMPDIR=%s %s/%s",
			filesDir,filesDir,filesDir,getReadableCmd(args));
		String[] cmd = {"su", "-c", exec};
		return cmd;
	}
	public String getReadableCmd(String args){
		return fileName + " " + args; 
	}

	private String fileName;
	private String filesDir;
}
