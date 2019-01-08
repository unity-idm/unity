/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import java.util.Optional;

import com.vaadin.server.Resource;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Settings useful for most of the credential reset screens
 * 
 * @author K. Benedyczak
 */
public class CredentialResetFlowConfig
{
	public final Optional<Resource> logo;
	public final UnityMessageSource msg;
	public final Runnable cancelCallback;
	public final float infoWidth;
	public final float contentsWidth;
	public final boolean compactLayout;
	
	public CredentialResetFlowConfig(Optional<Resource> logo, UnityMessageSource msg, Runnable cancelCallback,
			float infoWidth, float contentsWidth, boolean compactLayout)
	{
		this.logo = logo;
		this.msg = msg;
		this.cancelCallback = cancelCallback;
		this.infoWidth = infoWidth;
		this.contentsWidth = contentsWidth;
		this.compactLayout = compactLayout;
	}
}
