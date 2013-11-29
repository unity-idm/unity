/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

/**
 * Represents a remote identity
 * @author K. Benedyczak
 */
public class RemoteIdentity extends RemoteInformationBase
{
	private String identityType;
	
	public RemoteIdentity(String name, String type)
	{
		super(name);
		this.identityType = type;
	}

	public String getIdentityType()
	{
		return identityType;
	}

	public void setIdentityType(String identityType)
	{
		this.identityType = identityType;
	}
}
