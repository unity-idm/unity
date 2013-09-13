/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Helpers for JSON streaming API
 * @author K. Benedyczak
 */
public class JsonUtils
{
	public static void nextExpect(JsonParser jp, String name) throws IOException
	{
		jp.nextToken();
		String fieldName = jp.getCurrentName();
		if (!fieldName.equals(name))
			throw new IOException("Expected " + name + 
					" element, got: " + fieldName + 
					" " + jp.getCurrentLocation() + " tokenType: [" +
					jp.getCurrentToken() + "]");
		jp.nextToken();
	}
	
	public static void nextExpect(JsonParser jp, JsonToken tokenType) throws IOException
	{
		if (tokenType != jp.nextToken())
			throw new IOException("Expected " + tokenType + 
					", got: " + jp.getCurrentToken() + 
					" " + jp.getCurrentLocation());
	}
	
	public static void expect(JsonParser jp, JsonToken tokenType) throws IOException
	{
		if (tokenType != jp.getCurrentToken())
			throw new IOException("Expected " + tokenType + 
					", got: " + jp.getCurrentToken() + 
					" " + jp.getCurrentLocation());
	}
}
