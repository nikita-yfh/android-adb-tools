package ru.nikita.adb;

import java.lang.Exception;
import java.lang.String;

public class FastbootException extends RuntimeException {
	public FastbootException(String error) {
		super(getErrorMessage(error));
	}
	private static String getErrorMessage(String error) {
		if(error.startsWith("FAIL"))
			return "Error: " + error.substring(4);
		return error;
	}

}
