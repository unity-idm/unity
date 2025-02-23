/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.rp.verificator.TokenStatus;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;

@PrototypeComponent
public class AccessTokenAndPasswordVerificator extends AbstractVerificator implements AccessTokenAndPasswordExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, AccessTokenAndPasswordVerificator.class);

	public static final String NAME = "local-oauth-rp";
	public static final String DESC = "Verifies local tokens";

	private final OAuthAccessTokenRepository tokensDAO;
	private final CredentialHelper credentialHelper;
	private final PasswordVerificator.Factory passwordVerificatorFactory;

	private LocalCredentialVerificator passwordVerificator;
	private LocalBearerTokenVerificator bearerTokenVerificator;
	private LocalOAuthRPProperties verificatorProperties;

	@Autowired
	public AccessTokenAndPasswordVerificator(OAuthAccessTokenRepository tokensDAO,
			PasswordVerificator.Factory passwordVerificator, CredentialHelper credentialHelper)
	{
		super(NAME, DESC, AccessTokenAndPasswordExchange.ID);
		this.tokensDAO = tokensDAO;
		this.passwordVerificatorFactory = passwordVerificator;
		this.credentialHelper = credentialHelper;
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
		passwordVerificator = getLocalVerificator(verificatorProperties.getValue(LocalOAuthRPProperties.CREDENTIAL));
	}

	private LocalCredentialVerificator getLocalVerificator(String credential)
	{
		CredentialVerificator verificator = passwordVerificatorFactory.newInstance();
		verificator.setIdentityResolver(identityResolver);
		verificator.setInstanceName(NAME);

		Optional<CredentialDefinition> credDef = getCredentialDefinition(credentialHelper, credential);
		if (!credDef.isPresent())
		{
			throw new InternalException(
					"Invalid configuration of the verificator, local credential " + credential + " is undefined");
		}
		verificator.setSerializedConfiguration(credDef.get().getConfiguration());
		verificator.setIdentityResolver(identityResolver);
		LocalCredentialVerificator localVerificator = (LocalCredentialVerificator) verificator;
		localVerificator.setCredentialName(credential);
		return localVerificator;
	}

	public Optional<CredentialDefinition> getCredentialDefinition(CredentialHelper credentialHelper, String credential)
	{
		try
		{
			return Optional.ofNullable(credentialHelper.getCredentialDefinitions().get(credential));
		} catch (EngineException e)
		{
			throw new InternalException("Can not get credential definitions", e);
		}
	}

	@Override
	public AuthenticationResult checkTokenAndPassword(BearerAccessToken token, String username, String password)
			throws AuthenticationException
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
			log.debug("HTTP Bearer access token verification result: " + tokenVerificationResult.result.getStatus());
			return tokenVerificationResult.result;
		}
		AuthenticationResult localPasswordVerificationResult;

		try
		{
			localPasswordVerificationResult = checkPassword(username, password);
		} catch (Exception e)
		{
			log.debug("HTTP BASIC credential is invalid");
			return LocalAuthenticationResult.failed(e);
		}

		if (!localPasswordVerificationResult.getStatus().equals(Status.success))
		{
			log.debug("HTTP BASIC credential verification result: " + localPasswordVerificationResult.getStatus());
			return localPasswordVerificationResult;
		}

		if (!tokenVerificationResult.token.get().getClientId().get()
				.equals(localPasswordVerificationResult.getSuccessResult().authenticatedEntity.getEntityId()))
		{
			log.debug("Authenticated client {} is not matching the token's client entity {}",
					localPasswordVerificationResult.getSuccessResult().authenticatedEntity.getEntityId(),
					tokenVerificationResult.token.get().getClientId().get());
			return LocalAuthenticationResult.failed();
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

	private AuthenticationResult checkPassword(String username, String password) throws AuthenticationException
	{
		PasswordExchange passExchange = (PasswordExchange) passwordVerificator;
		return passExchange.checkPassword(username, password, null, false, null);
	}

	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.unkwown;
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<AccessTokenAndPasswordVerificator> factory)
		{
			super(NAME, DESC, factory);
		}
	}

}
