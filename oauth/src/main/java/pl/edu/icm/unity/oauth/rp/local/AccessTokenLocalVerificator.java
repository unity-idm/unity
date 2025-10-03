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
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.oauth.rp.verificator.TokenStatus;

@PrototypeComponent
public class AccessTokenLocalVerificator extends AbstractVerificator implements AccessTokenExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, AccessTokenLocalVerificator.class);

	public static final String NAME = "local-oauth-rp";
	public static final String DESC = "Verifies locally issued OAuth tokens";

	private final OAuthAccessTokenRepository tokensDAO;

	private LocalBearerTokenVerificator bearerTokenVerificator;
	private LocalOAuthRPProperties verificatorProperties;

	@Autowired
	public AccessTokenLocalVerificator(OAuthAccessTokenRepository tokensDAO)
	{
		super(NAME, DESC, AccessTokenExchange.ID);
		this.tokensDAO = tokensDAO;
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Mixed;
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
	public AuthenticationResult checkToken(BearerAccessToken token)
	{
		AuthenticationResultWithTokenStatus tokenVerificationResult;
		try
		{
			tokenVerificationResult = bearerTokenVerificator.checkToken(token);
		} catch (Exception e)
		{
			log.debug("HTTP Bearer access token is invalid or its processing failed", e);
			return LocalAuthenticationResult.failed(e);
		}

		if (!tokenVerificationResult.result.getStatus().equals(Status.success))
		{
			log.debug("HTTP Bearer access token verification result: {}", tokenVerificationResult.result.getStatus());
			return tokenVerificationResult.result;
		}
		updateInvocationContext(tokenVerificationResult.token.get());
		return tokenVerificationResult.result;
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
		public Factory(ObjectFactory<AccessTokenLocalVerificator> factory)
		{
			super(NAME, DESC, factory);
		}
	}
}
