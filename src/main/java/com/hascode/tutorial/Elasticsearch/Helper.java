package com.hascode.tutorial.Elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Helper {
	
	public static  String getCurrentDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		return sdf.format(new Date());
	}


}
