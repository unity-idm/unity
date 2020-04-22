/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.fido;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.yubico.webauthn.*;
import com.yubico.webauthn.attestation.Attestation;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.FidoManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.FidoException;
import pl.edu.icm.unity.types.authn.FidoCredentialInfo;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.UserHandle;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service processing FIDO registration and authentication functionality.
 * Do not store new credential in DB.
 * <p>
 * TODO should it update signature count? Depends on authenticator implementation
 *
 * @author R. Ledzinski
 */
@Component
public class FidoManagementImpl implements FidoManagement
{
    private static final Logger log = Log.getLogger(Log.U_SERVER_REST, FidoManagementImpl.class);
    private final UnityFidoRegistrationStorage fidoStorage;

    // JSON mapper
    static final ObjectMapper FIDO_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .registerModule(new Jdk8Module());

    // In memory storage for requests to be used when registration or authentication is finalized
    private final ConcurrentHashMap<String, PublicKeyCredentialCreationOptions> registrationRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AssertionRequest> authenticationRequests = new ConcurrentHashMap<>();

    private UnityMessageSource msg;
    private FidoEntityHelper entityHelper;

    @Autowired
    public FidoManagementImpl(final UnityMessageSource msg, final FidoEntityHelper entityHelper)
    {
        this.msg = msg;
        this.entityHelper = entityHelper;
        this.fidoStorage = new UnityFidoRegistrationStorage(entityHelper);
    }

    /**
     * Create registration request options that is passed to navigator.credentials.create() method on the client side.
     *
     * @return JSON registration options
     * @throws FidoException in case JSON creation error
     */
    @Override
    public SimpleEntry<String, String> getRegistrationOptions(final Long entityId, final String username) throws FidoException
    {
        Entity entity = entityHelper.getEntityOrThrow(entityId, username);

        // Create user handle when needed
        String userHandle = entityHelper.getOrCreateUserHandle(entity);

        String reqId = UUID.randomUUID().toString();

        PublicKeyCredentialCreationOptions registrationRequest = getRelyingParty().startRegistration(StartRegistrationOptions.builder()
                .user(UserIdentity.builder()
                        .name(entityHelper.getUsername(entity))
                        .displayName(entityHelper.getDisplayName(entity))
                        .id(new ByteArray(UserHandle.fromString(userHandle).getBytes()))
                        .build())
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
//						.userVerification(UserVerificationRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.DISCOURAGED) //FIXME need to be created based on credential definition configuration
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
        log.debug("Fido start registration for entityId: {}, username: {}, reqId: {}", entityId, username, reqId);
        return new SimpleEntry<>(reqId, json);
    }

    /**
     * Validates public key returned by navigator.credentials.create() method on the client side, create and return credentials.
     *
     * @param responseJson Authenticator response returned by navigator.credentials.create()
     * @return FidoCredentialInfo created credential
     * @throws FidoException In case of any registration problems
     */
    @Override
    public FidoCredentialInfo createFidoCredentials(final String reqId, final String responseJson) throws FidoException
    {
        log.debug("Fido finalize registration for reqId: {}", reqId);
        try
        {
            PublicKeyCredentialCreationOptions registrationRequest = registrationRequests.remove(reqId);
            if (registrationRequest == null)
                throw new FidoException(msg.getMessage("FidoExc.regReqExpired"));

            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                    PublicKeyCredential.parseRegistrationResponseJson(responseJson);
            RegistrationResult result = getRelyingParty().finishRegistration(FinishRegistrationOptions.builder()
                    .request(registrationRequest)
                    .response(pkc)
                    .build());
            logRegistrationDetails(registrationRequest, pkc, result);
            return createFidoCredentialInfo(registrationRequest, pkc, result);
        } catch (RegistrationFailedException | IOException e)
        {
            log.error("Registration failed. Exception: ", e);
            throw new FidoException(msg.getMessage("FidoExc.internalError"), e);
        }
    }

    /**
     * Create authentication request options that is passed to navigator.credentials.get() method on the client side.
     *
     * @return JSON authentication options
     * @throws FidoException In case of problems with JSON parsing
     */
    @Override
    public SimpleEntry<String, String> getAuthenticationOptions(final Long entityId, final String username) throws FidoException
    {
        Entity entity = entityHelper.getEntityOrThrow(entityId, username);
        String reqId = UUID.randomUUID().toString();

        // Check if user has FIDO2 credentials and UserHandle Identity
        if (fidoStorage.getFidoCredentialInfoForUserHandle(entityHelper.getUserHandle(entity)).isEmpty())
            throw new FidoException(msg.getMessage("FidoExc.noFidoCredential"));

        AssertionRequest authenticationRequest = getRelyingParty().startAssertion(
                StartAssertionOptions.builder()
                        .username(entityHelper.getUsername(entity))
                        .timeout(60000)
                        .userVerification(UserVerificationRequirement.DISCOURAGED) // FIXME need to be created based on credential definition configuration
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
    }

    private FidoCredentialInfo createFidoCredentialInfo(PublicKeyCredentialCreationOptions request,
                                                        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc,
                                                        RegistrationResult result)
    {
        Optional<Attestation> attestationMetadata = result.getAttestationMetadata();

        return FidoCredentialInfo.builder()
                .registrationTime(System.currentTimeMillis())
                .credentialId(result.getKeyId().getId())
                .publicKeyCose(result.getPublicKeyCose())
                .signatureCount(pkc.getResponse().getParsedAuthenticatorData().getSignatureCounter())
				.userPresent(pkc.getResponse().getParsedAuthenticatorData().getFlags().UP)
				.userVerified(pkc.getResponse().getParsedAuthenticatorData().getFlags().UV)
				.attestationFormat(pkc.getResponse().getAttestation().getFormat())
				.aaguid(pkc.getResponse().getParsedAuthenticatorData().getAttestedCredentialData().map(AttestedCredentialData::getAaguid).orElse(null))
                .attestationMetadata(attestationMetadata.orElse(null))
                .build();
    }

    private RelyingParty getRelyingParty()
    {
        // FIXME should be created based on credential definition configuration
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id("localhost")
                        .name("Unity IdM")
                        .build())
                .credentialRepository(fidoStorage)
                //.attestationConveyancePreference(AttestationConveyancePreference.DIRECT) // ask for attestation (authenticator) detail
                .attestationConveyancePreference(AttestationConveyancePreference.NONE) // any authenticator is OK
                .allowUntrustedAttestation(true)
                .allowOriginPort(true) // TODO should be false on production to allow only 443 port - make it configurable
                .build();
    }

    // TODO Remove later on. For now for debugging purposes
    private void logRegistrationDetails(PublicKeyCredentialCreationOptions request,
                                        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc,
                                        RegistrationResult result
    )
    {
        RegisteredCredential credential = RegisteredCredential.builder()
                .credentialId(result.getKeyId().getId())
                .userHandle(request.getUser().getId())
                .publicKeyCose(result.getPublicKeyCose()) // public key CBOR encoded
                .signatureCount(pkc.getResponse().getParsedAuthenticatorData().getSignatureCounter())
                .build();

        log.debug("userIdentity: {}", request.getUser());
        log.debug("credential: {}", credential);

        log.debug("authenticator.flags: {}", pkc.getResponse().getParsedAuthenticatorData().getFlags());
        log.debug("authenticator.aaguid: {}", pkc.getResponse().getParsedAuthenticatorData().getAttestedCredentialData().isPresent() ?
                pkc.getResponse().getParsedAuthenticatorData().getAttestedCredentialData().get().getAaguid() : "no attestedCredentialData");
        log.debug("response: {}", pkc.getResponse()); // encryption algorithm details and attestation signature

        log.debug("result.attestationTrusted: {}", result.isAttestationTrusted());
        log.debug("result.attestationType: {}", result.getAttestationType());
        log.debug("attestationMetadata: {}", result.getAttestationMetadata());
    }
}
