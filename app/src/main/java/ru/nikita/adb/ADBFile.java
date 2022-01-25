package ru.nikita.adb;

import java.lang.String;
import ru.nikita.adb.ADBTask;
import ru.nikita.adb.Binary;
import ru.nikita.adb.Device;
import android.util.Log;

public class ADBFile {
	private ADBFile(Binary adb, Device device, String path, boolean isDirectory){
		m_path = path;
		m_adb = adb;
		m_device = device;
		m_isDirectory = isDirectory;
	}
	public ADBFile(Binary adb, Device device, String path){
		this(adb, device, path, true);
	}
	public String getName(){
		return m_path.substring(m_path.lastIndexOf('/') + 1);
	}
	public String getParent(){
		return m_path.substring(0, m_path.lastIndexOf('/'));
	}
	public String getPath(){
		return m_path;
	}
	public void delete(){
		new ADBTask(m_adb).shell(m_device, String.format("rm -rf '%s'", m_path));
	}
	public void rename(String newName){
		m_path = getParent() + "/" + newName;
	}
	public boolean isDirectory(){
		return m_isDirectory;
	}

	public ADBFile[] listFiles(){
		String lsout = "";
		try{ lsout += new ADBTask(m_adb).shell(m_device, String.format("cd / && ls -ldA '%s'/*", m_path));
		}catch(Exception e){}
		try{ lsout += new ADBTask(m_adb).shell(m_device, String.format("cd / && ls -ldA '%s'/.*", m_path));
		}catch(Exception e){}
		
		String[] fileList = lsout.split("\n");

		ADBFile[] files = new ADBFile[fileList.length-1];
		for(int i = 0; i < fileList.length-1 ; i++){
			fileList[i] = fileList[i].trim();
			char flag = fileList[i].charAt(0);
			boolean isDir = (flag == 'd' || flag == 'l');
			String[] words = fileList[i].split(" ", 8);
			files[i] = new ADBFile(m_adb, m_device, words[7], isDir);
		}
		return files;
	}

	public void pull(String dest){
		new ADBTask(m_adb).pull(m_device, m_path, dest);
	}

	private Binary m_adb;
	private String m_path;
	private Device m_device;
	private boolean m_isDirectory;

}

