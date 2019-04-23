/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.uiproviders;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.providers.AdminUITabProvider;

/**
 * Provides OAuthToken UI component to home UI
 * @author P.Piernik
 */
@PrototypeComponent
public class OAuthTokenAdminUIProvider implements AdminUITabProvider
{	
	public static final String ID = "oauthAdminTokens";
	private SecuredTokensManagement tokenMan;
	private UnityMessageSource msg;
	private EntityManagement entityManagement;
	
	@Autowired
	public OAuthTokenAdminUIProvider(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			EntityManagement entityManagement)
	{
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.entityManagement = entityManagement;
	}

	@Override
	public Component getUI()
	{
		return new AdminTokensComponent(tokenMan, msg, entityManagement, true);
	}

	@Override
	public String getLabelKey()
	{
		return "OAuthTokenAdminUI.tokensLabel";
	}

	@Override
	public String getDescriptionKey()
	{
		return "OAuthTokenAdminUI.tokensDesc";
	}

	@Override
	public String getId()
	{
		return ID;
	}
}
