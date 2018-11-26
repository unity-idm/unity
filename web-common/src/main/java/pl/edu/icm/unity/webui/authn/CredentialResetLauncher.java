/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Optional;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;

/**
 * Implementation allows individual credential retrievals to launch credential reset process (of their choice).
 * 
 * @author K. Benedyczak
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
	
	static class CredentialResetUIConfig
	{
		public final Optional<Resource> logo;
		public final Runnable finishCallback;
		public final float infoWidth;
		public final float contentsWidth;
		public final boolean compactLayout;
		
		public CredentialResetUIConfig(Optional<Resource> logo, Runnable finishCallback, float infoWidth,
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
