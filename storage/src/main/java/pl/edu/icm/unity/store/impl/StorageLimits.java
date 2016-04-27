/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl;


/**
 * Provides shared storage limits, ensuring that our dat model can be put in all supported storage engines.
 * @author K. Benedyczak
 */
public class StorageLimits
{
	public static final int NAME_LIMIT = 200;
	public static final int CONTENTS_LIMIT = 60000;
	
	public static void checkNameLimit(String ofWhat) throws IllegalArgumentException
	{
		if (ofWhat != null && ofWhat.length() > NAME_LIMIT)
			throw new IllegalArgumentException("Name length must not exceed " + 
					NAME_LIMIT + " characters");

	}
	
	public static void checkContentsLimit(byte[] ofWhat) throws IllegalArgumentException
	{
		if (ofWhat != null && ofWhat.length > CONTENTS_LIMIT)
			throw new IllegalArgumentException("Contents must not exceed " + 
					CONTENTS_LIMIT + " bytes");

	} 
}
