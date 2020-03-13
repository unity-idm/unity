/******************************************************************************
 * Copyright (c) 2019, T-Mobile US.
 * <p>
 * All Rights Reserved
 * <p>
 * This is unpublished proprietary source code of T-Mobile US.
 * <p>
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *******************************************************************************/
package pl.edu.icm.unity.engine.fido;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.attestation.Attestation;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.FidoService;
import pl.edu.icm.unity.exceptions.FidoException;

import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initial service to process FIDO registration and authentication functionality.
 * Stores FIDO credentials in memory.
 *
 * @author R. Ledzinski
 */
@Component
public class FidoServiceImpl implements FidoService {
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, FidoServiceImpl.class);
	InMemoryRegistrationStorage userStorage = new InMemoryRegistrationStorage();

	// FIXME Should be created based on configuration
	private RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
			.id("localhost")
			.name("Unity IdM")
			.build();

	// FIXME should be created based on detailed configuration - likely to be dynamic. Credential definition configuration?
	private RelyingParty rp = RelyingParty.builder()
			.identity(rpIdentity)
			.credentialRepository(userStorage)
			// .attestationConveyancePreference(AttestationConveyancePreference.DIRECT) // ask for authenticator detail
			.attestationConveyancePreference(AttestationConveyancePreference.NONE) // any authenticator is OK
			.allowUntrustedAttestation(true)
			.allowOriginPort(true) // TODO should be false on production to allow only 443 port - make it configurable
			.build();

	// In memory storage for requests to be used when registration or authentication is finalized
	private ConcurrentHashMap<String, PublicKeyCredentialCreationOptions> registrationRequests = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, AssertionRequest> authenticationRequests = new ConcurrentHashMap<>();

	// JSON mapper
	private final ObjectMapper jsonMapper = new ObjectMapper()
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
			.registerModule(new Jdk8Module());

	/**
	 * Create registration request options that is passed to navigator.credentials.create() method on the client side.
	 * // FIXME - need to be linked with Unity entity.
	 *
	 * @return JSON registration options
	 * @throws FidoException in case JSON creation error
	 */
	@Override
	public SimpleEntry<String, String> getRegistrationOptions(final String username) throws FidoException
	{
		String reqId = UUID.randomUUID().toString();
		Random random = new Random();
		byte[] userHandle = new byte[64];
		random.nextBytes(userHandle);

		// FIXME need to be created based on configuration. Credential definition configuration?
		PublicKeyCredentialCreationOptions registrationRequest = rp.startRegistration(StartRegistrationOptions.builder()
				.user(UserIdentity.builder()
						.name(username)
						.displayName("Remi L.")
						.id(new ByteArray(userHandle))
						.build())
				.authenticatorSelection(AuthenticatorSelectionCriteria.builder()
//						.userVerification(UserVerificationRequirement.REQUIRED)
						.userVerification(UserVerificationRequirement.DISCOURAGED)
						.build())
				.build());
		String json;
		try {
			json = jsonMapper.writeValueAsString(registrationRequest);
			registrationRequests.put(reqId, registrationRequest);
		} catch (JsonProcessingException e) {
			throw new FidoException("Failed to create registration options", e);
		}
		log.debug("Fido start registration for username: {}, reqId: {}", username, reqId);
		return new SimpleEntry<>(reqId, json);
	}

	/**
	 * Validates public key returned by navigator.credentials.create() method on the client side and store credentials.
	 *
	 * @param responseJson Authenticator response returned by navigator.credentials.create()
	 * @throws FidoException In case of any registration problems
	 */
	@Override
	public void registerFidoCredentials(final String reqId, final String responseJson) throws FidoException
	{
		log.debug("Fido finalize registration for reqId: {}", reqId);
		try {
			PublicKeyCredentialCreationOptions registrationRequest = registrationRequests.remove(reqId);
			if (registrationRequest == null) {
				throw new FidoException("Registration request with given ID has expired.");
			}
			PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
					PublicKeyCredential.parseRegistrationResponseJson(responseJson);
			RegistrationResult result = rp.finishRegistration(FinishRegistrationOptions.builder()
					.request(registrationRequest)
					.response(pkc)
					.build());
			storeCredentials(registrationRequest.getUser().getName(), registrationRequest, pkc, result);
		} catch (RegistrationFailedException | IOException e) {
			log.error("Registration failed. Exception: ", e);
			throw new FidoException("Registration failed: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Create authentication request options that is passed to navigator.credentials.get() method on the client side.
	 * // FIXME - need to be linked with Unity entity.
	 *
	 * @return JSON authentication options
	 * @throws FidoException In case of problems with JSON parsing
	 */
	@Override
	public SimpleEntry<String, String> getAuthenticationOptions(final String username) throws FidoException
	{
		String reqId = UUID.randomUUID().toString();
		Random random = new Random();
		byte[] challage = new byte[32];
		random.nextBytes(challage);
		// FIXME need to be created based on configuration. Credential definition configuration?
		AssertionRequest authenticationRequest = rp.startAssertion(
						StartAssertionOptions.builder()
								.username(username)
								.timeout(60000)
								.userVerification(UserVerificationRequirement.DISCOURAGED)
								.build());
		String json = null;
		try {
			json = jsonMapper.writeValueAsString(authenticationRequest);
			authenticationRequests.put(reqId, authenticationRequest);
		} catch (JsonProcessingException e) {
			throw new FidoException("Failed to create authentication options", e);
		}
		log.debug("Fido start authentication for username: {}, reqId: {}", username, reqId);
		return new SimpleEntry<>(reqId, json);
	}

	/**
	 * Validates signatures made by Authenticator.
	 *
	 * @param jsonBody Authenticator response returned by navigator.credentials.get()
	 * @throws FidoException In case of any authentication problems
	 */
	@Override
	public void verifyAuthentication(final String reqId, final String jsonBody) throws FidoException
	{
		log.debug("Fido finalize authentication for reqId: {}", reqId);
		AssertionResult result;
		AssertionRequest authenticationRequest = authenticationRequests.remove(reqId);
		if (authenticationRequest == null) {
			throw new FidoException("Authentication request with given ID has expired.");
		}
		try {

			PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
					PublicKeyCredential.parseAssertionResponseJson(jsonBody);
			result = rp.finishAssertion(FinishAssertionOptions.builder()
					.request(authenticationRequest)
					.response(pkc)
					.build());
		} catch (AssertionFailedException | IOException e) {
			log.error("Authentication failed", e);
			throw new FidoException("Authentication failed: " + e.getLocalizedMessage(), e);
		}

		if (!result.isSuccess()) {
			throw new FidoException("Fido authentication failed.");
		}
	}

	private void storeCredentials(String username,
								  PublicKeyCredentialCreationOptions request,
								  PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc,
								  RegistrationResult result
								  ) {
		UserIdentity userIdentity = request.getUser();
		long signatureCount = pkc.getResponse().getParsedAuthenticatorData().getSignatureCounter();
		RegisteredCredential credential = RegisteredCredential.builder()
				.credentialId(result.getKeyId().getId())
				.userHandle(userIdentity.getId())
				.publicKeyCose(result.getPublicKeyCose()) // public key CBOR encoded
				.signatureCount(signatureCount)
				.build();

		Optional<Attestation> attestationMetadata = result.getAttestationMetadata();
		CredentialRegistration reg = CredentialRegistration.builder()
				.userIdentity(userIdentity)
				.registrationTime(Instant.now())
				.credential(credential)
				.signatureCount(signatureCount)
				.attestationMetadata(attestationMetadata)
				.build();

		// FIXME logged details to be removed
		log.debug("userIdentity: {}", userIdentity);
		log.debug("credential: {}", credential);

		log.debug("authenticator.flags: {}", pkc.getResponse().getParsedAuthenticatorData().getFlags());
		log.debug("authenticator.aaguid: {}", pkc.getResponse().getParsedAuthenticatorData().getAttestedCredentialData().isPresent() ?
				pkc.getResponse().getParsedAuthenticatorData().getAttestedCredentialData().get().getAaguid() : "no attestedCredentialData");
		log.debug("response: {}", pkc.getResponse().getAttestation().getAttestationStatement()); // encryption algorithm details and attestation signature
		pkc.getResponse().getAttestation().getAttestationStatement();

		log.debug("result.attestationTrusted: {}", result.isAttestationTrusted());
		log.debug("result.attestationType: {}", result.getAttestationType());
		log.debug("attestationMetadata: {}", attestationMetadata);

		userStorage.addRegistrationByUsername(userIdentity.getName(), reg);
	}

	// FIXME to be removed later on
	@Override
	public void removeFidoCredentials()
	{
		log.debug("Fido clear credentials");
		userStorage.removeAllRegistrations(null);
	}
}
