/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

class ClientAttributesProvider
{
	private final OAuthRequestValidator requestValidator;

	ClientAttributesProvider(OAuthRequestValidator requestValidator)
	{
		this.requestValidator = requestValidator;
	}

	Map<String, AttributeExt> getClientAttributes(EntityParam entity) throws OAuthErrorException
	{
		try
		{
			return requestValidator.getAttributesNoAuthZ(entity);
		} catch (Exception e)
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
	}

	String getClientName(EntityParam entity) throws OAuthErrorException
	{
		Map<String, AttributeExt> attributes = getClientAttributes(entity);
		AttributeExt nameA = attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME);
		if (nameA != null)
			return ((String) nameA.getValues().get(0));
		else
			return null;
	}

	@Component
	static class ClientAttributesProviderFactory
	{
		private final OAuthRequestValidatorFactory requestValidatorFactory;

		@Autowired
		ClientAttributesProviderFactory(OAuthRequestValidatorFactory requestValidatorFactory)
		{
			this.requestValidatorFactory = requestValidatorFactory;
		}

		ClientAttributesProvider getClientAttributeProvider(OAuthASProperties config)
		{
			return new ClientAttributesProvider(requestValidatorFactory.getOAuthRequestValidator(config));

		}

	}

}
