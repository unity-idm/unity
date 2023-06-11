/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;

@Component
class OAuthTokenStatisticPublisherFactory
{

	private final ApplicationEventPublisher eventPublisher;
	private final MessageSource msg;
	private final EntityManagement idMan;
	private final OAuthRequestValidatorFactory requestValidatorFactory;
	private final EndpointManagement endpointMan;
	private final LastIdPClinetAccessAttributeManagement lastIdPClinetAccessAttributeManagement;
	private final AttributesManagement unsecureAttributesMan;

	@Autowired
	OAuthTokenStatisticPublisherFactory(ApplicationEventPublisher eventPublisher, MessageSource msg,
			@Qualifier("insecure") EntityManagement idMan, OAuthRequestValidatorFactory requestValidator,
			@Qualifier("insecure") EndpointManagement endpointMan,
			LastIdPClinetAccessAttributeManagement lastIdPClinetAccessAttributeManagement,
			@Qualifier("insecure") AttributesManagement unsecureAttributesMan)
	{
		this.eventPublisher = eventPublisher;
		this.msg = msg;
		this.idMan = idMan;
		this.requestValidatorFactory = requestValidator;
		this.endpointMan = endpointMan;
		this.lastIdPClinetAccessAttributeManagement = lastIdPClinetAccessAttributeManagement;
		this.unsecureAttributesMan = unsecureAttributesMan;
	}

	OAuthTokenStatisticPublisher getOAuthTokenStatisticPublisher(OAuthASProperties oauthConfig,
			ResolvedEndpoint endpoint)
	{
		return new OAuthTokenStatisticPublisher(eventPublisher, msg, idMan,
				requestValidatorFactory.getOAuthRequestValidator(oauthConfig), endpointMan,
				lastIdPClinetAccessAttributeManagement, unsecureAttributesMan, oauthConfig, endpoint);
	}

}