/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.rp.verificator.TokenStatus;

@PrototypeComponent
public class LocalAccessTokenVerificator extends AbstractVerificator implements LocalAccessTokenExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, LocalAccessTokenVerificator.class);

	public static final String NAME = "local-oauth-rp";
	public static final String DESC = "OAuth 2 verifying local tokens";

	private final OAuthAccessTokenRepository tokensDAO;

	private LocalBearerTokenVerificator bearerTokenVerificator;
	private LocalOAuthRPProperties verificatorProperties;

	@Autowired
	public LocalAccessTokenVerificator(OAuthAccessTokenRepository tokensDAO)
	{
		super(NAME, DESC, LocalAccessTokenExchange.ID);
		this.tokensDAO = tokensDAO;
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			verificatorProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize OAuth RP verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(config));
			verificatorProperties = new LocalOAuthRPProperties(properties);
		} catch (ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the Local OAuth RP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the Local OAuth RP verificator(?)", e);
		}

		bearerTokenVerificator = new LocalBearerTokenVerificator(tokensDAO, verificatorProperties);
	}

	@Override
	public AuthenticationResult checkToken(BearerAccessToken token) throws AuthenticationException
	{
		AuthenticationResultWithTokenStatus verification;
		try
		{
			verification = bearerTokenVerificator.checkToken(token);
		} catch (AuthenticationException e)
		{
			log.debug("HTTP Bearer access token is invalid or its processing failed", e);
			throw e;
		}

		if (verification.token.isPresent() && verification.result.getStatus() == Status.success)
		{
			updateInvocationContext(verification.token.get());
		}

		return verification.result;
	}

	private void updateInvocationContext(TokenStatus status)
	{
		InvocationContext current = InvocationContext.getCurrent();
		current.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		current.setScopes(status.getScope().toStringList());
	}

	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.UNKNOWN;
	}

	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<LocalAccessTokenVerificator> factory)
		{
			super(NAME, DESC, factory);
		}
	}
}
