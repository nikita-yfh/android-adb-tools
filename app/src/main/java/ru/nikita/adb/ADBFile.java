package ru.nikita.adb;

import java.io.File;
import java.lang.String;
import ru.nikita.adb.ADBTask;
import ru.nikita.adb.Binary;

class ADBFile extends File{
	ADBFile(Binary adb, String path){
		super(path);
		this.adb=adb;
	}
	Binary adb;
}

