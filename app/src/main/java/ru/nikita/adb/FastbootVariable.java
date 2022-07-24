package ru.nikita.adb;

import java.lang.String;
import java.io.Serializable;

public class FastbootVariable implements Serializable{
	FastbootVariable(String name, String value) {
		this.name = name;
		this.value = value;
	}
	FastbootVariable(String output) {
		output = output.substring(4).trim();
		int first = output.lastIndexOf(':');
		this.name = output.substring(0, first);
		if(first == output.length() - 1)
			this.value = new String();
		else
			this.value = output.substring(first + 2);
	}
	public String name;
	public String value;
}

