/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.net.URL;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.Autowired;

import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.SAMLResponseValidatorUtil;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

@PrototypeComponent
public class SAMLResponseVerificator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLResponseVerificator.class);
	private final String responseConsumerAddress;
	private final ReplayAttackChecker replayAttackChecker;
	private final RemoteAuthnResultTranslator translator;
	
	@Autowired
	public SAMLResponseVerificator(
			ReplayAttackChecker replayAttackChecker,
			SharedEndpointManagement sharedEndpointManagement,
			AdvertisedAddressProvider advertisedAddrProvider,
			RemoteAuthnResultTranslator translator)
	{
		this(replayAttackChecker, 
				assembleResponseConsumerAddress(sharedEndpointManagement, advertisedAddrProvider), 
				translator);
	}

	public SAMLResponseVerificator(
			ReplayAttackChecker replayAttackChecker,
			String responseConsumerAddress,
			RemoteAuthnResultTranslator translator)
	{
		this.replayAttackChecker = replayAttackChecker;
		this.translator = translator;
		this.responseConsumerAddress = responseConsumerAddress;
	}
	
	private static String assembleResponseConsumerAddress(SharedEndpointManagement sharedEndpointManagement,
			AdvertisedAddressProvider advertisedAddrProvider)
	{
		URL baseAddress = advertisedAddrProvider.get();
		String baseContext = sharedEndpointManagement.getBaseContextPath();
		return baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
	}

	
	AuthenticationResult processResponse(RedirectedAuthnState remoteAuthnState, TranslationProfile profile)
	{
		RemoteAuthnContext castedState = (RemoteAuthnContext) remoteAuthnState;
		try
		{
			return verifySAMLResponse(castedState, profile);
		} catch (Exception e)
		{
			log.error("Runtime error during SAML response processing or principal mapping", e);
			return RemoteAuthenticationResult.failed(null, e,
					new ResolvableError("WebSAMLRetrieval.authnFailedError"));
		}
	}
	
	private AuthenticationResult verifySAMLResponse(RemoteAuthnContext context, TranslationProfile profile)
	{
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(context);
			return translator.getTranslatedResult(input, profile, 
					context.getAuthenticationTriggeringContext().isSandboxTriggered(), 
					Optional.empty(),
					context.getRegistrationFormForUnknown(),
					context.isEnableAssociation());
		} catch (RemoteAuthenticationException e)
		{
			log.info("SAML response verification or processing failed", e);
			return RemoteAuthenticationResult.failed(e.getResult().getRemotelyAuthenticatedPrincipal(), e,
					new ResolvableError("WebSAMLRetrieval.authnFailedError"));
		}
	}
	
	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(RemoteAuthnContext context) 
			throws RemoteAuthenticationException 
	{
		ResponseDocument responseDocument;
		try
		{
			responseDocument = ResponseDocument.Factory.parse(context.getResponse());
		} catch (XmlException e)
		{
			throw new RemoteAuthenticationException("The SAML response can not be parsed - " +
					"XML data is corrupted", e);
		}
		
		SAMLResponseValidatorUtil responseValidatorUtil = new SAMLResponseValidatorUtil(
				context.getSpConfiguration(), replayAttackChecker, responseConsumerAddress);
		SamlTrustChecker trustChecker = context.getSpConfiguration().getTrustCheckerForIdP(context.getIdp());
		RemotelyAuthenticatedInput input = responseValidatorUtil.verifySAMLResponse(responseDocument, 
				context.getVerifiableResponse(),
				context.getRequestId(), 
				SAMLBindings.valueOf(context.getResponseBinding().toString()), 
				context.getGroupAttribute(), context.getIdp(), trustChecker);
		return input;
	}
}


