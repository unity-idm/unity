/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity;

import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Various useful appliactaion wide constatnts
 * @author K. Benedyczak
 */
public class Constants
{
	public static final Charset UTF = Charset.forName("UTF-8");
	
	//TODO - remove this, use injected instance
	public static final ObjectMapper MAPPER = new ObjectMapper();
}
