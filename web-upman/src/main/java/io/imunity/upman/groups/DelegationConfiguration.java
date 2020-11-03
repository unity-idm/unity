/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman.groups;

class DelegationConfiguration
{
	final boolean enabled;
	final String logoUrl;
	
	DelegationConfiguration(boolean enabled, String logoUrl)
	{
		this.enabled = enabled;
		this.logoUrl = logoUrl;
	}		
}