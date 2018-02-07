/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.cxf.interceptor.Fault;
import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.validator.UnityAuthnRequestValidator;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Implementation of the SAML authentication protocol over SOAP.
 *  
 * @author K. Benedyczak
 */
public class SAMLAuthnImpl implements SAMLAuthnInterface
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLAuthnImpl.class);
	protected SamlIdpProperties samlProperties;
	protected String endpointAddress;
	protected IdPEngine idpEngine;
	protected PreferencesManagement preferencesMan;
	protected AttributeTypeSupport aTypeSupport;
	

	public SAMLAuthnImpl(AttributeTypeSupport aTypeSupport,
			SamlIdpProperties samlProperties, String endpointAddress,
			IdPEngine idpEngine, PreferencesManagement preferencesMan)
	{
		this.aTypeSupport = aTypeSupport;
		this.samlProperties = samlProperties;
		this.endpointAddress = endpointAddress;
		this.idpEngine = idpEngine;
		this.preferencesMan = preferencesMan;
	}

	@Override
	public ResponseDocument authnRequest(AuthnRequestDocument reqDoc)
	{
		if (log.isTraceEnabled())
			log.trace("Received SAML AuthnRequest: " + reqDoc.xmlText());
		SAMLAuthnContext context = new SAMLAuthnContext(reqDoc, samlProperties);
		try
		{
			validate(context);
		} catch (SAMLServerException e1)
		{
			log.debug("Throwing SAML fault, caused by validation exception", e1);
			throw new Fault(e1);
		}
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, context);
		NameIDType samlRequester = context.getRequest().getIssuer();
		
		ResponseDocument respDoc;
		try
		{
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			SPSettings spPreferences = preferences.getSPSettings(samlRequester);

			TranslationResult userInfo = getUserInfo(samlProcessor);
			IdentityParam selectedIdentity = getIdentity(userInfo, samlProcessor, spPreferences);
			log.debug("Authentication of " + selectedIdentity);
			Collection<Attribute> attributes = samlProcessor.getAttributes(userInfo, spPreferences);
			respDoc = samlProcessor.processAuthnRequest(selectedIdentity, attributes, 
					context.getResponseDestination());
		} catch (Exception e)
		{
			log.debug("Throwing SAML fault, caused by processing exception", e);
			SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
			respDoc = samlProcessor.getErrorResponse(convertedException);
		}
		if (log.isTraceEnabled())
			log.trace("Returning SAML Response: " + respDoc.xmlText());
		return respDoc;
	}

	protected TranslationResult getUserInfo(AuthnResponseProcessor processor) 
			throws EngineException
	{
		String profile = samlProperties.getValue(CommonIdPProperties.TRANSLATION_PROFILE);
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		return idpEngine.obtainUserInformationWithEnrichingImport(new EntityParam(ae.getEntityId()), 
				processor.getChosenGroup(), profile, 
				processor.getIdentityTarget(), Optional.empty(),
				"SAML2", SAMLConstants.BINDING_SOAP,
				processor.isIdentityCreationAllowed(),
				samlProperties);
	}

	
	protected IdentityParam getIdentity(TranslationResult userInfo, AuthnResponseProcessor samlProcessor, 
			SPSettings preferences) 
			throws EngineException, SAMLRequesterException
	{
		List<IdentityParam> validIdentities = samlProcessor.getCompatibleIdentities(userInfo.getIdentities());
		return idpEngine.getIdentity(validIdentities, preferences.getSelectedIdentity());
	}
	
	protected void validate(SAMLAuthnContext context) throws SAMLServerException
	{
		UnityAuthnRequestValidator validator = new UnityAuthnRequestValidator(endpointAddress, 
				samlProperties.getSoapTrustChecker(), samlProperties.getRequestValidity(), 
				samlProperties.getReplayChecker());
		
		validator.validate(context.getRequestDocument());
	}
}
