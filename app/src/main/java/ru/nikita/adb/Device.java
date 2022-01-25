package ru.nikita.adb;

import java.lang.String;
import java.io.Serializable;

class Device implements Serializable{
	Device(String id){
		this(id, null);
	}
	Device(String id, String state){
		this.id=id;
		this.state=state;
	}
	public String id;
	public String state;
}
