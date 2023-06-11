/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import io.imunity.fido.FidoExchange;
import io.imunity.fido.credential.FidoCredential;
import io.imunity.fido.credential.FidoCredentialInfo;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.imunity.fido.service.FidoEntityHelper.NO_ENTITY_MSG;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Service for processing FIDO registration and authentication functionality.
 * Does not store new credential in DB.
 * <p>
 *
 * @author R. Ledzinski
 */
@PrototypeComponent
public class FidoCredentialVerificator extends AbstractLocalVerificator implements FidoExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FIDO, FidoCredentialVerificator.class);
	public static final String NAME = "fido";
	public static final String DESC = "Verifies fido credential";

	private final UnityFidoRegistrationStorage.UnityFidoRegistrationStorageCache fidoStorage;
	FidoCredential credential = new FidoCredential();

	// JSON mapper
	static final ObjectMapper FIDO_MAPPER = new ObjectMapper()
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
			.registerModule(new Jdk8Module());

	// In memory storage for requests to be used when authentication is finalized
	private final ConcurrentHashMap<String, AssertionRequest> authenticationRequests = new ConcurrentHashMap<>();

	private final MessageSource msg;
	private final FidoEntityHelper entityHelper;
	private final CredentialHelper credentialHelper;
	private final AdvertisedAddressProvider addressProvider;

	@Autowired
	public FidoCredentialVerificator(MessageSource msg, 
			FidoEntityHelper entityHelper, 
			CredentialHelper credentialHelper,
			UnityFidoRegistrationStorage.UnityFidoRegistrationStorageCache fidoStorage, 
			AdvertisedAddressProvider addressProvider)
	{
		super(NAME, DESC, FidoExchange.ID, false);
		this.msg = msg;
		this.entityHelper = entityHelper;
		this.credentialHelper = credentialHelper;
		this.fidoStorage = fidoStorage;
		this.addressProvider = addressProvider;
	}

	@Override
	public String getSerializedConfiguration()
	{
		return credential.serialize();
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		credential = FidoCredential.deserialize(json);
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential, boolean verify)
			throws IllegalCredentialException
	{
		return rawCredential;
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential)
	{
		String credDetails = isNull(currentCredential) ? "" : currentCredential;
		return new CredentialPublicInformation(credDetails.isEmpty() ? 
				LocalCredentialState.notSet : LocalCredentialState.correct, credDetails);
	}

	@Override
	public String invalidate(String currentCredential)
	{
		throw new IllegalStateException("This credential doesn't support invalidation");
	}

	@Override
	public boolean isCredentialSet(EntityParam entity)
			throws EngineException
	{
		return credentialHelper.isCredentialSet(entity, credentialName);
	}

	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		return false;
	}

	@Override
	public SimpleEntry<String, String> getAuthenticationOptions(final Long entityId, final String username) throws FidoException
	{
		String reqId = UUID.randomUUID().toString();
		AssertionRequest authenticationRequest = getRelyingParty().startAssertion(getAssertionOptions(entityId, username));

		String json = null;
		try
		{
			json = FIDO_MAPPER.writeValueAsString(authenticationRequest);
			authenticationRequests.put(reqId, authenticationRequest);
		} catch (JsonProcessingException e)
		{
			log.error("Parsing JSON: {}, exception: ", json, e);
			throw new FidoException(msg.getMessage("FidoExc.internalError"), e);
		}
		log.info("Fido start authentication for entityId: {}, username: {}, reqId: {}", entityId, username, reqId);
		return new SimpleEntry<>(reqId, json);
	}

	private StartAssertionOptions getAssertionOptions(final Long entityId, final String username) {
		StartAssertionOptions.StartAssertionOptionsBuilder builder = StartAssertionOptions.builder()
				.timeout(60000)
				.userVerification(UserVerificationRequirement.valueOf(credential.getUserVerification()));

		String assertionUsername = getAssertionUsername(entityId, username);

		if (nonNull(assertionUsername))
			builder.username(assertionUsername);

		return builder.build();
	}

	private String getAssertionUsername(final Long entityId, final String username) {
		Optional<Identities> resolvedUsername = entityHelper.resolveUsername(entityId, username);
		if (resolvedUsername.isPresent())
			return assertFidoCredentialExists(resolvedUsername.get());

		if (!credential.isLoginLessAllowed())
			throw new NoEntityException(msg.getMessage(NO_ENTITY_MSG));

		return null;
	}

	private String assertFidoCredentialExists(Identities resolvedUsername)
	{
		List<FidoCredentialInfo> credentials = fidoStorage.getInstance(getCredentialName())
				.getFidoCredentialInfoForUsername(resolvedUsername.getUsername());
		if (credentials.isEmpty())
		{
			log.warn("No {} credential found for user {}", getCredentialName(), resolvedUsername.getUsername());
			throw new NoEntityException(msg.getMessage("Fido.invalidUsername"));
		}
		// Make sure UserHandle Identity exists - it may be missing during first login after opt-in using registration form
		entityHelper.getOrCreateUserHandle(resolvedUsername, credentials.get(0).getUserHandle());
		return resolvedUsername.getUsername();
	}

	@Override
	public AuthenticationResult verifyAuthentication(final String reqId, final String jsonBody) throws FidoException
	{
		log.debug("Fido finalize authentication for reqId: {}", reqId);
		AssertionResult result;
		String username = null;

		try
		{
			PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
					PublicKeyCredential.parseAssertionResponseJson(jsonBody);
			AssertionRequest authenticationRequest = authenticationRequests.remove(reqId);
			if (authenticationRequest == null)
				throw new FidoException(msg.getMessage("FidoExc.authReqExpired"));
			result = getRelyingParty().finishAssertion(FinishAssertionOptions.builder()
					.request(authenticationRequest)
					.response(pkc)
					.build());
			if (result.isSuccess())
			{
				username = result.getUsername();
				updateSignatureCount(username, pkc.getId(), 
						pkc.getResponse().getParsedAuthenticatorData().getSignatureCounter());
			}
		} catch (AssertionFailedException e)
		{
			throw new FidoException(msg.getMessage("Fido.authFailed", e));
		} catch (IOException | EngineException e)
		{
			log.error("Authentication failed with exception", e);
			throw new FidoException(msg.getMessage("FidoExc.internalError"), e);
		}

		if (!result.isSuccess())
			throw new FidoException(msg.getMessage("Fido.authFailed"));

		Identities resolvedUsername = entityHelper.resolveUsername(null, username)
				.orElseThrow(() -> new NoEntityException(msg.getMessage(NO_ENTITY_MSG)));
		AuthenticatedEntity ae = new AuthenticatedEntity(entityHelper.getEntityId(resolvedUsername.getEntityParam()), username, null);

		return LocalAuthenticationResult.successful(ae);
	}

	private void updateSignatureCount(String username, ByteArray credentialId, long signatureCount) throws EngineException
	{
		Identities identities = entityHelper.resolveUsername(null, username)
				.orElseThrow(() -> new NoEntityException(msg.getMessage(NO_ENTITY_MSG)));
		List<FidoCredentialInfo> credentials = fidoStorage.getInstance(getCredentialName())
				.getFidoCredentialInfoForUsername(identities.getUsername());
		List<FidoCredentialInfo> newCredentials = credentials.stream()
				.map(c -> {
					if (c.getCredentialId().equals(credentialId))
					{
						log.debug("SignCount: old={}, new={}", c.getSignatureCount(), signatureCount);
						return c.copyBuilder().signatureCount(signatureCount).build();
					}
					else
						return c;
				})
				.collect(Collectors.toList());
		credentialHelper.updateCredential(entityHelper.getEntityId(identities.getEntityParam()), 
				getCredentialName(), FidoCredentialInfo.serializeList(newCredentials));
	}

	private RelyingParty getRelyingParty()
	{
		return getRelyingParty(addressProvider.get().getHost(), fidoStorage.getInstance(credentialName), credential);
	}

	static RelyingParty getRelyingParty(String hostName, UnityFidoRegistrationStorage storage, 
			FidoCredential credentialConfiguration)
	{
		return RelyingParty.builder()
				.identity(RelyingPartyIdentity.builder()
						.id(hostName)
						.name(credentialConfiguration.getHostName())
						.build())
				.credentialRepository(storage)
				.attestationConveyancePreference(AttestationConveyancePreference.valueOf(credentialConfiguration.getAttestationConveyance()))
				.allowUntrustedAttestation(true)
				.allowOriginPort(true)
				.allowUnrequestedExtensions(true)
				.build();
	}

	/**
	 * Factory that creates instances of FidoCredentialVerificator - separate for each credential definition.
	 */
	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<FidoCredentialVerificator> factory)
		{
			super(NAME, DESC, false, factory);
		}
	}
}
