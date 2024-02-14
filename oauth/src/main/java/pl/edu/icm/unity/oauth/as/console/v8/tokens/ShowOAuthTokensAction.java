/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.v8.tokens;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.spi.services.IdpServiceAdditionalAction;
import io.imunity.webconsole.spi.services.ServiceActionRepresentation;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.webui.common.Images;

@Component("ShowOAuthTokensActionV8")
class ShowOAuthTokensAction implements IdpServiceAdditionalAction
{
	public static final String NAME = "OAuthTokens";
	private MessageSource msg;
	private ObjectFactory<OAuthTokensComponentV8> actionComponentfactory;

	@Autowired
	ShowOAuthTokensAction(MessageSource msg, ObjectFactory<OAuthTokensComponentV8> factory)
	{
		this.msg = msg;
		this.actionComponentfactory = factory;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("ShowOAuthTokensAction.displayedName");
	}

	@Override
	public String getSupportedServiceType()
	{
		return OAuthAuthzWebEndpoint.Factory.TYPE.getName();
	}

	@Override
	public ServiceActionRepresentation getActionRepresentation()
	{
		return new ServiceActionRepresentation(msg.getMessage("ShowOAuthTokensAction.name"),
				Images.usertoken.getResource());
	}

	@Override
	public com.vaadin.ui.Component getActionContent(String serviceName)
	{
		return actionComponentfactory.getObject().forService(serviceName);
	}
}
