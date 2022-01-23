package ru.nikita.adb;

import java.lang.String;

class Device {
	Device(String id){
		this(id, null, null);
	}
	Device(String id, String name, String state){
		this.id=id;
		this.name=name;
		this.state=state;
	}
	public String id;
	public String name;
	public String state;
}
