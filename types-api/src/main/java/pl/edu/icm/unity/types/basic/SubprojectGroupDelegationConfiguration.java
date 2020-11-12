/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.types.basic;

public class SubprojectGroupDelegationConfiguration
{
	public final boolean enabled;
	public final String logoUrl;
	public final boolean enableSubprojects;
	
	public SubprojectGroupDelegationConfiguration(boolean enabled, boolean enableSubprojects,  String logoUrl)
	{
		
		this.enabled = enabled;
		this.logoUrl = logoUrl;
		this.enableSubprojects = enableSubprojects;
	}
}
