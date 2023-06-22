/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AttestedCredentialData;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.RegistrationFailedException;
import io.imunity.fido.FidoRegistration;
import io.imunity.fido.credential.FidoCredential;
import io.imunity.fido.credential.FidoCredentialInfo;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.imunity.fido.service.FidoCredentialVerificator.FIDO_MAPPER;
import static io.imunity.fido.service.FidoCredentialVerificator.getRelyingParty;
import static io.imunity.fido.service.FidoEntityHelper.NO_ENTITY_MSG;
import static java.util.Objects.isNull;

/**
 * Service for processing FIDO registration functionality.
 * Does not store new credential in DB.
 *
 * @author R. Ledzinski
 */
@Component
class FidoCredentialRegistrationVerificator implements FidoRegistration
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FIDO, FidoCredentialRegistrationVerificator.class);

	private final UnityFidoRegistrationStorage.UnityFidoRegistrationStorageCache fidoStorage;

	// In memory storage for requests to be used when registration is finalized
	private final ConcurrentHashMap<String, PublicKeyCredentialCreationOptions> registrationRequests = new ConcurrentHashMap<>();

	private MessageSource msg;
	private FidoEntityHelper entityHelper;
	private AdvertisedAddressProvider addressProvider;

	@Autowired
	public FidoCredentialRegistrationVerificator(MessageSource msg, FidoEntityHelper entityHelper,
			UnityFidoRegistrationStorage.UnityFidoRegistrationStorageCache fidoStorage,
			AdvertisedAddressProvider addressProvider)
	{
		this.msg = msg;
		this.entityHelper = entityHelper;
		this.fidoStorage = fidoStorage;
		this.addressProvider = addressProvider;
	}

	public SimpleEntry<String, String> getRegistrationOptions(String credentialName, String credentialConfiguration,
			Long entityId, String username, boolean useResidentKey) throws FidoException
	{
		Optional<Identities> resolvedUsername = entityHelper.resolveUsername(entityId, username);
		if (!resolvedUsername.isPresent() && (isNull(username) || username.isEmpty()))
			throw new NoEntityException(msg.getMessage(NO_ENTITY_MSG));
		String reqId = UUID.randomUUID().toString();
		FidoUserHandle userHandle = resolvedUsername.map(entityHelper::getOrCreateUserHandle).orElse(FidoUserHandle.create());
		String registrationUsername = resolvedUsername.map(Identities::getUsername).orElse(username);
		String displayName = resolvedUsername.map(entityHelper::getDisplayName).orElse(username);

		FidoCredential fidoCredential = FidoCredential.deserialize(credentialConfiguration);
		ResidentKeyRequirement residentKeyRequirement = fidoCredential.isLoginLessAllowed() && useResidentKey ? 
				ResidentKeyRequirement.PREFERRED : ResidentKeyRequirement.DISCOURAGED;
		PublicKeyCredentialCreationOptions registrationRequest = getRelyingParty(addressProvider.get().getHost(), 
				fidoStorage.getInstance(credentialName), fidoCredential)
				.startRegistration(StartRegistrationOptions.builder()
				.user(UserIdentity.builder()
						.name(registrationUsername)
						.displayName(displayName)
						.id(new ByteArray(userHandle.getBytes()))
						.build())
				.authenticatorSelection(AuthenticatorSelectionCriteria.builder()
						.userVerification(UserVerificationRequirement.valueOf(fidoCredential.getUserVerification()))
						.residentKey(residentKeyRequirement)
						.build())
				.build());

		String json;
		try
		{
			json = FIDO_MAPPER.writeValueAsString(registrationRequest);
			registrationRequests.put(reqId, registrationRequest);
		} catch (JsonProcessingException e)
		{
			throw new FidoException("Failed to create registration options", e);
		}
		log.info("Fido start registration for entityId: {}, username: {}, reqId: {} {}", entityId, username, reqId, json);
		return new SimpleEntry<>(reqId, json);
	}

	public FidoCredentialInfo createFidoCredentials(String credentialName, String credentialConfiguration,
			String reqId, String responseJson) throws FidoException
	{
		log.debug("Fido finalize registration for reqId: {}", reqId);
		try
		{
			PublicKeyCredentialCreationOptions registrationRequest = registrationRequests.remove(reqId);
			if (registrationRequest == null)
				throw new FidoException(msg.getMessage("FidoExc.regReqExpired"));

			PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
					PublicKeyCredential.parseRegistrationResponseJson(responseJson);
			RegistrationResult result = getRelyingParty(addressProvider.get().getHost(), 
						fidoStorage.getInstance(credentialName), 
						FidoCredential.deserialize(credentialConfiguration))
					.finishRegistration(FinishRegistrationOptions.builder()
					.request(registrationRequest)
					.response(pkc)
					.build());
			return createFidoCredentialInfo(pkc, registrationRequest, result);
		} catch (RegistrationFailedException | IOException e)
		{
			log.error("Registration failed. Exception: ", e);
			throw new FidoException(msg.getMessage("FidoExc.internalError"), e);
		}
	}

	private FidoCredentialInfo createFidoCredentialInfo(PublicKeyCredential<AuthenticatorAttestationResponse, 
			ClientRegistrationExtensionOutputs> pkc,
			PublicKeyCredentialCreationOptions registrationRequest, 
			RegistrationResult result)
	{
		return FidoCredentialInfo.builder()
				.registrationTime(System.currentTimeMillis())
				.credentialId(result.getKeyId().getId())
				.publicKeyCose(result.getPublicKeyCose())
				.signatureCount(pkc.getResponse().getParsedAuthenticatorData().getSignatureCounter())
				.userPresent(pkc.getResponse().getParsedAuthenticatorData().getFlags().UP)
				.userVerified(pkc.getResponse().getParsedAuthenticatorData().getFlags().UV)
				.attestationFormat(pkc.getResponse().getAttestation().getFormat())
				.aaguid(pkc.getResponse().getParsedAuthenticatorData().getAttestedCredentialData()
						.map(AttestedCredentialData::getAaguid).map(ByteArray::getHex).orElse(null))
				.userHandle(new FidoUserHandle(registrationRequest.getUser().getId().getBytes()).asString())
				.build();
	}
}
