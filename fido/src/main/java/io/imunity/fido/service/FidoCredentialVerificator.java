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
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import io.imunity.fido.FidoExchange;
import io.imunity.fido.credential.FidoCredential;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

/**
 * Service for processing FIDO registration and authentication functionality.
 * Does not store new credential in DB.
 * <p>
 * TODO should it update signature count? Depends on authenticator implementation
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
	public FidoCredentialVerificator(final MessageSource msg, final FidoEntityHelper entityHelper, final CredentialHelper credentialHelper,
									 final UnityFidoRegistrationStorage.UnityFidoRegistrationStorageCache fidoStorage, final AdvertisedAddressProvider addressProvider)
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
		return new CredentialPublicInformation(credDetails.isEmpty() ? LocalCredentialState.notSet : LocalCredentialState.correct, credDetails);
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
		Identities resolvedUsername = entityHelper.resolveUsername(entityId, username);

		// Check if user has FIDO2 credentials and UserHandle Identity
		if (fidoStorage.getInstance(getCredentialName()).getFidoCredentialInfoForUsername(resolvedUsername.getUsername()).isEmpty())
		{
			log.warn("No {} credential found for user {}", getCredentialName(), resolvedUsername.getUsername());
			throw new FidoException(msg.getMessage("FidoExc.noEntityForName"));
		}

		String reqId = UUID.randomUUID().toString();
		AssertionRequest authenticationRequest = getRelyingParty().startAssertion(
				StartAssertionOptions.builder()
						.username(resolvedUsername.getUsername())
						.timeout(60000)
						.userVerification(UserVerificationRequirement.valueOf(credential.getUserVerification()))
						.build());
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
		log.debug("Fido start authentication for entityId: {}, username: {}, reqId: {}", entityId, username, reqId);
		return new SimpleEntry<>(reqId, json);
	}

	@Override
	public AuthenticationResult verifyAuthentication(final String reqId, final String jsonBody) throws FidoException
	{
		log.debug("Fido finalize authentication for reqId: {}", reqId);
		AssertionResult result;
		AssertionRequest authenticationRequest = authenticationRequests.remove(reqId);
		if (authenticationRequest == null)
			throw new FidoException(msg.getMessage("FidoExc.authReqExpired"));

		try
		{

			PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
					PublicKeyCredential.parseAssertionResponseJson(jsonBody);
			result = getRelyingParty().finishAssertion(FinishAssertionOptions.builder()
					.request(authenticationRequest)
					.response(pkc)
					.build());
			// FIXME update signature counter
			log.debug("New signatureCounter={}", pkc.getResponse().getParsedAuthenticatorData().getSignatureCounter());
		} catch (AssertionFailedException e)
		{
			log.error("Authentication failed", e);
			throw new FidoException(msg.getMessage("FidoExc.authFailed", e));
		} catch (IOException e)
		{
			log.error("Authentication failed with exception", e);
			throw new FidoException(msg.getMessage("FidoExc.internalError"), e);
		}

		if (!result.isSuccess())
			throw new FidoException(msg.getMessage("FidoExc.authFailed"));

		String username = authenticationRequest.getUsername().orElseThrow(() -> new FidoException(msg.getMessage("FidoExc.internalError")));
		Identities resolvedUsername = entityHelper.resolveUsername(null, username);
		AuthenticatedEntity ae = new AuthenticatedEntity(entityHelper.getEntityId(resolvedUsername.getEntityParam()), username, null);


		return new AuthenticationResult(AuthenticationResult.Status.success, ae);
	}

	private RelyingParty getRelyingParty()
	{
		return getRelyingParty(addressProvider.get().getHost(), fidoStorage.getInstance(credentialName), credential);
	}

	static RelyingParty getRelyingParty(final String hostName, final UnityFidoRegistrationStorage storage, final FidoCredential credentialConfiguration)
	{
		return RelyingParty.builder()
				.identity(RelyingPartyIdentity.builder()
						.id(hostName)
						.name(hostName) // FIXME where to define it?
						.build())
				.credentialRepository(storage)
				.attestationConveyancePreference(AttestationConveyancePreference.valueOf(credentialConfiguration.getAttestationConveyance()))
				.allowUntrustedAttestation(true)
				.allowOriginPort(true)
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
