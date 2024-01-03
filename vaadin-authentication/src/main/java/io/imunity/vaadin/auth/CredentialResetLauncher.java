/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;

import java.util.Optional;

/**
 * Implementation allows individual credential retrievals to launch credential reset process (of their choice).
 */
public interface CredentialResetLauncher
{
	/**
	 * @return configuration of the reset flow, which is set globally on the endpoint
	 */
	CredentialResetUIConfig getConfiguration();
	
	/**
	 * Signals authentication UI that credential reset should be started, and provides a component serving it.
	 * @param credentialResetUI
	 */
	void startCredentialReset(Component credentialResetUI);
	
	class CredentialResetUIConfig
	{
		public final Optional<LocalOrRemoteResource> logo;
		public final Runnable finishCallback;
		public final float infoWidth;
		public final float contentsWidth;
		public final boolean compactLayout;
		
		public CredentialResetUIConfig(Optional<LocalOrRemoteResource> logo, Runnable finishCallback, float infoWidth,
				float contentsWidth, boolean compactLayout)
		{
			this.logo = logo;
			this.finishCallback = finishCallback;
			this.infoWidth = infoWidth;
			this.contentsWidth = contentsWidth;
			this.compactLayout = compactLayout;
		}
	}

}
