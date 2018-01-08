/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import java.util.Collection;
import java.util.Optional;

import org.apache.cxf.interceptor.Fault;
import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.webservice.SAMLQueryInterface;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAttributeQueryContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AttributeQueryResponseProcessor;
import pl.edu.icm.unity.saml.validator.UnityAttributeQueryValidator;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AssertionIDRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthzDecisionQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Implementation of the SAML Assertion Query and Request protocol, SOAP binding.
 * Only attributeQuery is supported as of now.
 * @author K. Benedyczak
 */
public class SAMLAssertionQueryImpl implements SAMLQueryInterface
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLAssertionQueryImpl.class);
	protected SamlIdpProperties samlProperties;
	protected String endpointAddress;
	protected IdPEngine idpEngine;
	protected PreferencesManagement preferencesMan;
	private AttributeTypeSupport aTypeSupport;
	
	public SAMLAssertionQueryImpl(AttributeTypeSupport aTypeSupport, 
			SamlIdpProperties samlProperties, String endpointAddress,
			IdPEngine idpEngine, PreferencesManagement preferencesMan)
	{
		super();
		this.aTypeSupport = aTypeSupport;
		this.samlProperties = samlProperties;
		this.endpointAddress = endpointAddress;
		this.idpEngine = idpEngine;
		this.preferencesMan = preferencesMan;
	}

	@Override
	public ResponseDocument attributeQuery(AttributeQueryDocument query)
	{
		if (log.isTraceEnabled())
			log.trace("Received SAML AttributeQuery: " + query.xmlText());
		SAMLAttributeQueryContext context = new SAMLAttributeQueryContext(query, samlProperties);
		try
		{
			validate(context);
		} catch (SAMLServerException e1)
		{
			log.debug("Throwing SAML fault, caused by validation exception", e1);
			throw new Fault(e1);
		}
		AttributeQueryResponseProcessor processor = new AttributeQueryResponseProcessor(aTypeSupport, context);
		ResponseDocument respDoc;
		try
		{
			IdentityTaV subjectId = processor.getSubjectsIdentity();
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan,
					new EntityParam(subjectId));
			NameIDType reqIssuer = query.getAttributeQuery().getIssuer();
			SPSettings spPreferences = preferences.getSPSettings(reqIssuer);
			Collection<Attribute> attributes = getAttributes(subjectId, processor, spPreferences);
			respDoc = processor.processAtributeRequest(attributes);
		} catch (SAMLRequesterException e1)
		{
			log.debug("Throwing SAML fault, caused by processing exception", e1);
			respDoc = processor.getErrorResponse(e1);
		} catch (Exception e)
		{
			log.debug("Throwing SAML fault, caused by processing exception", e);
			SAMLServerException convertedException = processor.convert2SAMLError(e, null, true);
			respDoc = processor.getErrorResponse(convertedException);
		}
		if (log.isTraceEnabled())
			log.trace("Returning SAML Response: " + respDoc.xmlText());
		return respDoc;
	}

	@Override
	public ResponseDocument assertionIDRequest(AssertionIDRequestDocument query)
	{
		throw new Fault(new SAMLResponderException("This SAML operation is not supported by this service"));
	}

	@Override
	public ResponseDocument authnQuery(AuthnQueryDocument query)
	{
		throw new Fault(new SAMLResponderException("This SAML operation is not supported by this service"));
	}

	@Override
	public ResponseDocument authzDecisionQuery(AuthzDecisionQueryDocument query)
	{
		throw new Fault(new SAMLResponderException("This SAML operation is not supported by this service"));
	}
	
	protected Collection<Attribute> getAttributes(IdentityTaV subjectId,
			AttributeQueryResponseProcessor processor, SPSettings preferences) throws EngineException
	{
		String profile = samlProperties.getValue(CommonIdPProperties.TRANSLATION_PROFILE);
		TranslationResult userInfo = idpEngine.obtainUserInformationWithEarlyImport(subjectId, 
				processor.getChosenGroup(), profile, 
				processor.getIdentityTarget(), Optional.empty(), 
				"SAML2", SAMLConstants.BINDING_SOAP, false,
				samlProperties);
		return processor.getAttributes(userInfo, preferences);
	}

	protected void validate(SAMLAttributeQueryContext context) throws SAMLServerException
	{
		UnityAttributeQueryValidator validator = new UnityAttributeQueryValidator(endpointAddress, 
				samlProperties.getSoapTrustChecker(), samlProperties.getRequestValidity(), 
				samlProperties.getReplayChecker());
		
		validator.validate(context.getRequestDocument());
	}

}
