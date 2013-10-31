/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Various useful application wide constants
 * @author K. Benedyczak
 */
public class Constants
{
	public static final Charset UTF = Charset.forName("UTF-8");
	
	public static final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	
	//TODO - remove this, use injected instance
	public static final ObjectMapper MAPPER = new ObjectMapper();
}
