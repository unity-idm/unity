/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.ws;

import java.util.Collection;
import java.util.Date;

import org.apache.cxf.interceptor.Fault;
import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import eu.unicore.security.etd.DelegationRestrictions;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.ws.SAMLAuthnImpl;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.unicore.samlidp.saml.AuthnWithETDResponseProcessor;
import pl.edu.icm.unity.unicore.samlidp.saml.SoapAuthWithETDRequestValidator;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Implementation of the SAML authentication protocol over SOAP.
 * <p>
 * This version is UNICORE aware: if request has X.500 issuer and required identity is as well X.500,
 * then a bootstrap ETD assertion is generated and added. 
 *  
 * @author K. Benedyczak
 */
public class SAMLETDAuthnImpl extends SAMLAuthnImpl implements SAMLAuthnInterface
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLETDAuthnImpl.class);

	public SAMLETDAuthnImpl(AttributeTypeSupport aTypeSupport,
			SamlIdpProperties samlProperties, String endpointAddress,
			IdPEngine idpEngine,
			PreferencesManagement preferencesMan)
	{
		super(aTypeSupport, samlProperties, endpointAddress, idpEngine, preferencesMan);
	}

	@Override
	public ResponseDocument authnRequest(AuthnRequestDocument reqDoc)
	{
		SAMLAuthnContext context = new SAMLAuthnContext(reqDoc, samlProperties);
		try
		{
			validate(context);
		} catch (SAMLServerException e1)
		{
			log.debug("Throwing SAML fault, caused by validation exception", e1);
			throw new Fault(e1);
		}
		
		AuthnWithETDResponseProcessor samlProcessor = new AuthnWithETDResponseProcessor(aTypeSupport, context);
		NameIDType samlRequester = context.getRequest().getIssuer();
		
		ResponseDocument respDoc;
		try
		{
			SamlPreferencesWithETD preferences = SamlPreferencesWithETD.getPreferences(preferencesMan);
			SPETDSettings spEtdPreferences = preferences.getSPETDSettings(samlRequester);
			SPSettings spPreferences = preferences.getSPSettings(samlRequester);
			
			TranslationResult userInfo = getUserInfo(samlProcessor);
			IdentityParam selectedIdentity = getIdentity(userInfo, samlProcessor, spPreferences);
			log.debug("Authentication of " + selectedIdentity);
			Collection<Attribute> attributes = samlProcessor.getAttributes(userInfo, spPreferences);
			respDoc = samlProcessor.processAuthnRequest(selectedIdentity, attributes, 
					context.getResponseDestination(),
					getRestrictions(spEtdPreferences));
		} catch (Exception e)
		{
			log.debug("Throwing SAML fault, caused by processing exception", e);
			SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
			respDoc = samlProcessor.getErrorResponse(convertedException);
		}
		return respDoc;
	}

	protected DelegationRestrictions getRestrictions(SPETDSettings spPreferences)
	{
		if (!spPreferences.isGenerateETD())
			return null;
		
		long ms = spPreferences.getEtdValidity();
		Date start = new Date();
		Date end = new Date(start.getTime() + ms);
		return new DelegationRestrictions(start, end, -1);
	}

	
	@Override
	protected void validate(SAMLAuthnContext context) throws SAMLServerException
	{
		SoapAuthWithETDRequestValidator validator = new SoapAuthWithETDRequestValidator(endpointAddress, 
				samlProperties.getSoapTrustChecker(), samlProperties.getRequestValidity(), 
				samlProperties.getReplayChecker());
		
		validator.validate(context.getRequestDocument());
	}
}
