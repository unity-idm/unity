/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Properties of a remote IdP which are relevant for showing authentication widget and organizing UI.
 * 
 * @author K. Benedyczak
 */
public class IdPVisalSettings
{
	public final String logoUrl;
	public final Set<String> tags;
	public final String name;
	
	public IdPVisalSettings(String logoUrl, Collection<String> tags, String name)
	{
		this.logoUrl = logoUrl;
		this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
		this.name = name;
	}
}
