/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset;

import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Optional;

/**
 * Settings useful for most of the credential reset screens
 */
public class CredentialResetFlowConfig
{
	public final Optional<LocalOrRemoteResource> logo;
	public final MessageSource msg;
	public final Runnable cancelCallback;
	public final float infoWidth;
	public final float contentsWidth;
	public final boolean compactLayout;
	
	public CredentialResetFlowConfig(Optional<LocalOrRemoteResource> logo, MessageSource msg, Runnable cancelCallback,
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
