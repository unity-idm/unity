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
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.providers.HomeUITabProvider;

/**
 * Provides OAuthToken UI component to home UI
 * @author P.Piernik
 */
@PrototypeComponent
public class OAuthTokenHomeUIProvider implements HomeUITabProvider
{	
	public static final String ID = "oauthTokens";
	
	private SecuredTokensManagement tokenMan;
	private UnityMessageSource msg;
	private EntityManagement entityManagement;
	
	@Autowired
	public OAuthTokenHomeUIProvider(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			EntityManagement entityManagement)
	{
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.entityManagement = entityManagement;
	}

	@Override
	public Component getUI()
	{
		return new UserHomeTokensComponent(tokenMan, msg, entityManagement);
	}

	@Override
	public String getLabelKey()
	{
		return  "OAuthTokenUserHomeUI.tokensLabel";
	}

	@Override
	public String getDescriptionKey()
	{
		return "OAuthTokenUserHomeUI.tokensDesc";
	}

	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public Images getIcon()
	{
		return Images.usertoken;
	}
}
