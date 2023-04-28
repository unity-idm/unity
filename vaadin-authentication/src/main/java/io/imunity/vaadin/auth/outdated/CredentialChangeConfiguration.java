/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.outdated;

/**
 * Configuration of credential change UI
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
