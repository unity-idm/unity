/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.util.Collection;
import java.util.Set;

/**
 * Properties of a remote IdP which are relevant for showing authentication widget and organizing UI.
 * 
 * @author K. Benedyczak
 */
public class IdPVisalSettings
{
	private final String DEFAULT_V8_LOGO = "file:../common/img/other/logo-hand.png";

	public final String logoURI;
	public final Set<String> tags;
	public final String name;
	public final String federationId;

	public IdPVisalSettings(String logoURI, Collection<String> tags, String name, String federationId)
	{
		this.logoURI = logoURI;
		this.tags = Set.copyOf(tags);
		this.name = name;
		this.federationId = federationId;
	}

	public String getLogoURI()
	{
		if(DEFAULT_V8_LOGO.equals(logoURI))
			return "../unitygw/img/other/logo-hand.png";
		return logoURI;
	}
}
