/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
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
import pl.edu.icm.unity.oauth.as.OAuthASFederationConfig;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.stdext.credential.NoCredentialResetImpl;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

@PrototypeComponent
public class PrivateKeyJwtVerificator extends AbstractVerificator implements ClientAssertionExchange, LocalCredentialVerificator
{
	public static final String NAME = "private-key-jwt";
	public static final String DESC = "Verifies OAuth2 client JWT assertions using JWKS";
	private static final String[] IDENTITY_TYPES = {UsernameIdentity.ID};
	private static final ResolvableError GENERIC_ERROR = new ResolvableError("PrivateKeyJwtVerificator.invalidAssertion");
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, PrivateKeyJwtVerificator.class);

	private ClientPublicKeysCredential credential = new ClientPublicKeysCredential();
	private String credentialName;
	private final CredentialHelper credentialHelper;
	private final OAuthEndpointsCoordinator coordinator;
	private final AttributesManagement attributesManagement;
	private final JwtClientAssertionVerifier jwtVerifier = new JwtClientAssertionVerifier();

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
		ObjectNode root = credential.getSerializedConfiguration();
		if (credentialName != null)
			root.put("credentialName", credentialName);
		return JsonUtil.serialize(root);
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		ObjectNode root = JsonUtil.parse(json);
		if (root.has("credentialName"))
			credentialName = root.get("credentialName").asText(null);
	}

	@Override
	public AuthenticationResult verifyClientAssertion(String clientAssertion, URI tokenEndpointUri)
	{
		SignedJWT jwt;
		try
		{
			jwt = SignedJWT.parse(clientAssertion);
		} catch (Exception e)
		{
			log.debug("Failed to parse client_assertion JWT", e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		String clientId;
		try
		{
			clientId = jwt.getJWTClaimsSet().getSubject();
			if (clientId == null)
			{
				log.debug("client_assertion JWT has no sub claim");
				return LocalAuthenticationResult.failed(GENERIC_ERROR);
			}
		} catch (Exception e)
		{
			log.debug("Failed to read claims from client_assertion JWT", e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		EntityWithCredential resolved;
		try
		{
			resolved = identityResolver.resolveIdentity(clientId, IDENTITY_TYPES, credentialName);
		} catch (Exception e)
		{
			log.info("Client entity not found for client_id: {}", clientId);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		Optional<OAuthASFederationConfig> federationConfig = coordinator.getFederationConfig(tokenEndpointUri.toString());
		if (federationConfig.isEmpty())
		{
			log.warn("No AS config registered for token endpoint {}", tokenEndpointUri);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
		try
		{
			Collection<AttributeExt> authnMethodAttrs = attributesManagement.getAttributes(
					new EntityParam(resolved.getEntityId()), federationConfig.get().clientsGroup(),
					OAuthSystemAttributesProvider.CLIENT_AUTHN_METHOD);
			if (authnMethodAttrs.isEmpty() || !ClientAuthnMethod.private_key_jwt.toString()
					.equals(authnMethodAttrs.iterator().next().getValues().get(0)))
			{
				log.info("Client {} does not have private_key_jwt authentication method configured", clientId);
				return LocalAuthenticationResult.failed(GENERIC_ERROR);
			}
		} catch (Exception e)
		{
			log.warn("Cannot read authentication method attribute for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		JWKSet jwkSet;
		try
		{
			String storedJwks = resolved.getCredentialValue();
			if (storedJwks == null || storedJwks.isBlank())
				throw new AuthenticationException("No JWKS configured for client " + clientId);
			jwkSet = JWKSet.parse(storedJwks);
		} catch (Exception e)
		{
			log.info("Failed to resolve JWKS for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		try
		{
			jwtVerifier.verifyJwt(jwt, jwkSet, tokenEndpointUri, clientId);
		} catch (AuthenticationException e)
		{
			log.info("JWT assertion verification failed for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}

		AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), clientId, null);
		return LocalAuthenticationResult.successful(ae, getAuthenticationMethod());
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
	public pl.edu.icm.unity.base.authn.CredentialPublicInformation checkCredentialState(String currentCredential)
			throws InternalException
	{
		if (currentCredential == null || currentCredential.isBlank())
			return new pl.edu.icm.unity.base.authn.CredentialPublicInformation(
					LocalCredentialState.notSet, "");
		return new pl.edu.icm.unity.base.authn.CredentialPublicInformation(
				LocalCredentialState.correct, "");
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
