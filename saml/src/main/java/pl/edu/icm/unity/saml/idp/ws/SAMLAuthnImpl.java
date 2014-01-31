/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.interceptor.Fault;
import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.validator.UnityAuthnRequestValidator;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.webservice.SAMLAuthnInterface;

/**
 * Implementation of the SAML authentication protocol over SOAP.
 *  
 * @author K. Benedyczak
 */
public class SAMLAuthnImpl implements SAMLAuthnInterface
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLAuthnImpl.class);
	protected SamlProperties samlProperties;
	protected String endpointAddress;
	protected IdentitiesManagement identitiesMan;
	protected AttributesManagement attributesMan;
	protected PreferencesManagement preferencesMan;
	

	public SAMLAuthnImpl(SamlProperties samlProperties, String endpointAddress,
			IdentitiesManagement identitiesMan, AttributesManagement attributesMan,
			PreferencesManagement preferencesMan)
	{
		this.samlProperties = samlProperties;
		this.endpointAddress = endpointAddress;
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
		this.preferencesMan = preferencesMan;
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
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(context);
		NameIDType samlRequester = context.getRequest().getIssuer();
		
		ResponseDocument respDoc;
		try
		{
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			SPSettings spPreferences = preferences.getSPSettings(samlRequester);

			Identity selectedIdentity = getIdentity(samlProcessor, spPreferences);
			Collection<Attribute<?>> attributes = getAttributes(samlProcessor, spPreferences);
			respDoc = samlProcessor.processAuthnRequest(selectedIdentity, attributes);
		} catch (Exception e)
		{
			log.debug("Throwing SAML fault, caused by processing exception", e);
			SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
			respDoc = samlProcessor.getErrorResponse(convertedException);
		}
		return respDoc;
	}

	protected Identity getIdentity(AuthnResponseProcessor samlProcessor, SPSettings preferences) 
			throws EngineException, SAMLRequesterException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		Entity authenticatedEntity = identitiesMan.getEntity(
				new EntityParam(ae.getEntityId()));
		List<Identity> validIdentities = samlProcessor.getCompatibleIdentities(authenticatedEntity);
		if (validIdentities.size() > 0)
		{
			for (Identity id: validIdentities)
			{
				if (id.getComparableValue().equals(preferences.getSelectedIdentity()))
				{
					return id;
				}
			}
		}
		return validIdentities.get(0);
	}
	
	protected Collection<Attribute<?>> getAttributes(AuthnResponseProcessor processor, SPSettings preferences) 
			throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		EntityParam entity = new EntityParam(ae.getEntityId());
		Collection<String> allGroups = identitiesMan.getGroups(entity);
		Collection<AttributeExt<?>> allAttribtues = attributesMan.getAttributes(
				entity, processor.getChosenGroup(), null);
		Map<String, Attribute<?>> all = processor.prepareReleasedAttributes(allAttribtues, allGroups);
		Set<String> hidden = preferences.getHiddenAttribtues();
		for (String hiddenA: hidden)
			all.remove(hiddenA);
		return all.values();
	}

	
	protected void validate(SAMLAuthnContext context) throws SAMLServerException
	{
		UnityAuthnRequestValidator validator = new UnityAuthnRequestValidator(endpointAddress, 
				samlProperties.getSoapTrustChecker(), samlProperties.getRequestValidity(), 
				samlProperties.getReplayChecker());
		
		validator.validate(context.getRequestDocument());
	}
}
