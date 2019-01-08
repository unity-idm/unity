/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.outdated;

/**
 * Configuration of credential change UI
 * 
 * @author K. Benedyczak
 */
public class CredentialChangeConfiguration
{
	public final String logoURL;
	public final float width;
	public final boolean compactLayout;
	
	public CredentialChangeConfiguration(String logoURL, float width, boolean compactLayout)
	{
		this.logoURL = logoURL;
		this.width = width;
		this.compactLayout = compactLayout;
	}
}
