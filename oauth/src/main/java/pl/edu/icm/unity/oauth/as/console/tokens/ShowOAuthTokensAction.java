/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.spi.IdpServiceAdditionalAction;
import io.imunity.console.spi.ServiceActionRepresentation;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;

@Component
class ShowOAuthTokensAction implements IdpServiceAdditionalAction
{
	public static final String NAME = "OAuthTokens";
	private final MessageSource msg;
	private final ObjectFactory<OAuthTokensComponent> actionComponentfactory;

	@Autowired
	ShowOAuthTokensAction(MessageSource msg, ObjectFactory<OAuthTokensComponent> factory)
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
				VaadinIcon.TAGS);
	}

	@Override
	public com.vaadin.flow.component.Component getActionContent(String serviceName)
	{
		return actionComponentfactory.getObject().forService(serviceName);
	}
}
