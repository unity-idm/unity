/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;


public class RoleImpl implements AuthzRole
{
	private String name;
	private String description;
	private AuthzCapability[] capabilities;
	private AuthzCapability[] allCapabilities;

	public RoleImpl(String name, String description, AuthzCapability[] capabilities)
	{
		this(name, description, capabilities, new AuthzCapability[0]);
	}

	public RoleImpl(String name, String description, AuthzCapability[] capabilities, 
			AuthzCapability[] selfAccessCapabilities)
	{
		this.name = name;
		this.description = description;
		this.capabilities = capabilities;
		this.allCapabilities = new AuthzCapability[capabilities.length + selfAccessCapabilities.length];
		int i=0;
		for (; i<capabilities.length; i++)
			allCapabilities[i] = capabilities[i];
		for (int j=0; j<selfAccessCapabilities.length; j++, i++)
			allCapabilities[i] = selfAccessCapabilities[j];
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	@Override
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	@Override
	public AuthzCapability[] getCapabilities(boolean selfAccess)
	{
		return selfAccess ? allCapabilities : capabilities;
	}
	
	public void setCapabilities(AuthzCapability[] capabilities)
	{
		this.capabilities = capabilities;
	}
}
