/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Various useful application wide constants
 * @author K. Benedyczak
 */
public class Constants
{
	public static final String SIMPLE_DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
	
	//TODO - remove this, use injected instance
	public static final ObjectMapper MAPPER = new ObjectMapper();
	
	public static final DateTimeFormatter DT_FORMATTER_MEDIUM = 
			DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
}
