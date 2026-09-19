/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn.local;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWKSet;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.federation.OAuthASFederationConfig;
import pl.edu.icm.unity.oauth.as.token.authn.ClientAssertionExchange;
import pl.edu.icm.unity.oauth.as.token.authn.ClientAssertionVerificationFlow;
import pl.edu.icm.unity.oauth.as.token.authn.JwtClientAssertionVerifier;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.stdext.credential.NoCredentialResetImpl;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

@PrototypeComponent
public class PrivateKeyJwtVerificator extends AbstractVerificator
		implements ClientAssertionExchange, LocalCredentialVerificator
{
	public static final String NAME = "private-key-jwt";
	public static final String DESC = "Verifies OAuth2 client JWT assertions using JWKS";
	private static final String[] IDENTITY_TYPES = {UsernameIdentity.ID};
	private static final ResolvableError GENERIC_ERROR = new ResolvableError("PrivateKeyJwtVerificator.invalidAssertion");
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, PrivateKeyJwtVerificator.class);

	private ClientPublicKeysCredential credential = new ClientPublicKeysCredential();
	private String credentialName;
	private int clockSkewSeconds = (int) JwtClientAssertionVerifier.DEFAULT_CLOCK_SKEW.toSeconds();
	private int maxAssertionLifetimeSeconds = (int) JwtClientAssertionVerifier.DEFAULT_MAX_ASSERTION_LIFETIME.toSeconds();
	private final CredentialHelper credentialHelper;
	private final OAuthEndpointsCoordinator coordinator;
	private final AttributesManagement attributesManagement;
	private final ClientAssertionVerificationFlow verificationFlow = new ClientAssertionVerificationFlow();

	@Autowired
	public PrivateKeyJwtVerificator(CredentialHelper credentialHelper, OAuthEndpointsCoordinator coordinator,
			@Qualifier("insecure") AttributesManagement attributesManagement)
	{
		super(NAME, DESC, ClientAssertionExchange.ID);
		this.credentialHelper = credentialHelper;
		this.coordinator = coordinator;
		this.attributesManagement = attributesManagement;
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Mixed;
	}

	@Override
	public String getCredentialName()
	{
		return credentialName;
	}

	@Override
	public void setCredentialName(String credentialName)
	{
		this.credentialName = credentialName;
	}

	@Override
	public String getSerializedConfiguration()
	{
		Properties raw = new Properties();
		if (credentialName != null)
			raw.put(PrivateKeyJwtAuthenticatorProperties.PREFIX + PrivateKeyJwtAuthenticatorProperties.CREDENTIAL_NAME,
					credentialName);
		raw.put(PrivateKeyJwtAuthenticatorProperties.PREFIX + PrivateKeyJwtAuthenticatorProperties.ALLOWED_CLOCK_SKEW,
				String.valueOf(clockSkewSeconds));
		raw.put(PrivateKeyJwtAuthenticatorProperties.PREFIX + PrivateKeyJwtAuthenticatorProperties.MAX_ASSERTION_LIFETIME,
				String.valueOf(maxAssertionLifetimeSeconds));
		StringWriter writer = new StringWriter();
		try
		{
			raw.store(writer, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize private-key-jwt authenticator configuration", e);
		}
		return writer.toString();
	}

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		Properties raw = UnityPropertiesHelper.parse(source == null ? "" : source);
		PrivateKeyJwtAuthenticatorProperties props;
		try
		{
			props = new PrivateKeyJwtAuthenticatorProperties(raw);
		} catch (ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the private-key-jwt authenticator", e);
		}
		if (props.isSet(PrivateKeyJwtAuthenticatorProperties.CREDENTIAL_NAME))
			credentialName = props.getValue(PrivateKeyJwtAuthenticatorProperties.CREDENTIAL_NAME);
		clockSkewSeconds = props.getIntValue(PrivateKeyJwtAuthenticatorProperties.ALLOWED_CLOCK_SKEW);
		verificationFlow.setClockSkew(Duration.ofSeconds(clockSkewSeconds));
		maxAssertionLifetimeSeconds = props.getIntValue(PrivateKeyJwtAuthenticatorProperties.MAX_ASSERTION_LIFETIME);
		verificationFlow.setMaxAssertionLifetime(Duration.ofSeconds(maxAssertionLifetimeSeconds));
	}

	@Override
	public AuthenticationResult verifyClientAssertion(String clientAssertion, URI tokenEndpointUri)
	{
		Optional<OAuthEndpointsCoordinator.FederationConfigEntry> configEntry =
				coordinator.findFederationConfigByPath(tokenEndpointUri.getPath());
		if (configEntry.isEmpty())
		{
			log.warn("No AS config registered for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
		OAuthASFederationConfig federationConfig = configEntry.get().config();
		URI canonicalUri = URI.create(configEntry.get().canonicalUrl());
		return verificationFlow.verify(clientAssertion, canonicalUri, GENERIC_ERROR,
				clientId -> resolveLocalJwks(clientId, federationConfig));
	}

	private ClientAssertionVerificationFlow.JwksResolution resolveLocalJwks(String clientId,
			OAuthASFederationConfig federationConfig) throws Exception
	{
		EntityWithCredential resolved;
		try
		{
			resolved = identityResolver.resolveIdentity(clientId, IDENTITY_TYPES, credentialName);
		} catch (Exception e)
		{
			log.info("Client entity not found for client_id: {}", clientId);
			throw e;
		}
		Collection<AttributeExt> authnMethodAttrs;
		try
		{
			authnMethodAttrs = attributesManagement.getAttributes(
					new EntityParam(resolved.getEntityId()), federationConfig.clientsGroup(),
					OAuthSystemAttributesProvider.CLIENT_AUTHN_METHOD);
		} catch (Exception e)
		{
			log.warn("Cannot read authentication method attribute for client {}: {}", clientId, e.getMessage());
			throw e;
		}
		if (authnMethodAttrs.isEmpty() || !ClientAuthnMethod.private_key_jwt.toString()
				.equals(authnMethodAttrs.iterator().next().getValues().get(0)))
		{
			log.info("Client {} does not have private_key_jwt authentication method configured", clientId);
			throw new AuthenticationException("Wrong authn method for client " + clientId);
		}
		String storedJwks = resolved.getCredentialValue();
		if (storedJwks == null || storedJwks.isBlank())
		{
			log.info("Failed to resolve JWKS for client {}: no JWKS stored", clientId);
			throw new AuthenticationException("No JWKS for client " + clientId);
		}
		try
		{
			return new ClientAssertionVerificationFlow.JwksResolution(
					resolved.getEntityId(), JWKSet.parse(storedJwks));
		} catch (Exception e)
		{
			log.info("Failed to resolve JWKS for client {}: {}", clientId, e.getMessage());
			throw e;
		}
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential, boolean verify)
			throws InternalException
	{
		return credential.prepareForStorage(rawCredential);
	}

	@Override
	public String invalidate(String currentCredential)
	{
		return "";
	}

	public CredentialReset getCredentialResetBackend()
	{
		return new NoCredentialResetImpl();
	}

	@Override
	public boolean isSupportingInvalidation()
	{
		return false;
	}

	@Override
	public Optional<String> updateCredentialAfterConfigurationChange(String currentCredential)
	{
		return Optional.empty();
	}

	@Override
	public boolean isCredentialSet(EntityParam entity) throws EngineException
	{
		return credentialHelper.isCredentialSet(entity, credentialName);
	}

	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		return false;
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential)
			throws InternalException
	{
		if (currentCredential == null || currentCredential.isBlank())
			return new CredentialPublicInformation(
					LocalCredentialState.notSet, "");
		return new CredentialPublicInformation(
				LocalCredentialState.correct, new PrivateKeyJwtExtraInfo(currentCredential).toJson());
	}

	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.UNKNOWN;
	}

	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<PrivateKeyJwtVerificator> factory)
		{
			super(NAME, DESC, false, factory);
		}
	}
}
