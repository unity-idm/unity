/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Helpers for JSON streaming API
 * @author K. Benedyczak
 */
public class JsonUtils
{
	public static void nextExpect(JsonParser jp, String name) throws IOException
	{
		jp.nextToken();
		String fieldName = jp.currentName();
		if (!fieldName.equals(name))
			throw new IOException("Expected " + name + 
					" element, got: " + fieldName + 
					" " + jp.currentLocation() + " tokenType: [" +
					jp.currentToken() + "]");
		jp.nextToken();
	}
	
	
	public static void expect(JsonParser jp, String name) throws IOException
	{
		String fieldName = jp.currentName();
		if (!fieldName.equals(name))
			throw new IOException("Expected " + name + 
					" element, got: " + fieldName + 
					" " + jp.currentLocation() + " tokenType: [" +
					jp.currentToken() + "]");
		jp.nextToken();
	}
	
	public static void nextExpect(JsonParser jp, JsonToken tokenType) throws IOException
	{
		if (tokenType != jp.nextToken())
			throw new IOException("Expected " + tokenType + 
					", got: " + jp.currentToken() + 
					" " + jp.currentLocation());
	}
	
	public static void expect(JsonParser jp, JsonToken tokenType) throws IOException
	{
		if (tokenType != jp.currentToken())
			throw new IOException("Expected " + tokenType + 
					", got: " + jp.currentToken() + 
					" " + jp.currentLocation());
	}
	
	public static ArrayNode deserialize2Array(JsonParser input, String expectedCategory) throws IOException
	{
		nextExpect(input, expectedCategory);
		return input.readValueAsTree();
	}
}
