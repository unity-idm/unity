/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic representation of a remotely obtained information, used for subclassing.
 *   
 * @author K. Benedyczak
 */
public abstract class RemoteInformationBase
{
	public static final String UNITY_GROUP = "unity-group";
	public static final String UNITY_ATTRIBUTE = "unity-attribute";
	public static final String UNITY_ATTRIBUTE_VISIBILITY = "unity-attribute-visibility";
	public static final String UNITY_IDENTITY = "unity-identity";
	public static final String UNITY_IDENTITY_TYPE = "unity-identity-type";
	public static final String UNITY_IDENTITY_CREDREQ = "unity-identity-credreq";
	
	private String name;
	private Map<String, String> metadata;
	
	public RemoteInformationBase(String name)
	{
		this.name = name;
		this.metadata = new HashMap<>();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Map<String, String> getMetadata()
	{
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata)
	{
		this.metadata = metadata;
	}
}
