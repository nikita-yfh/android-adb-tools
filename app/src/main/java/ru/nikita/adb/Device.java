package ru.nikita.adb;

import java.lang.String;

class Device {
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
