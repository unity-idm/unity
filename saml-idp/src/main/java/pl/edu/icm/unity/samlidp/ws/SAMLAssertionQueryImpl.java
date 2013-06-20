/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.ws;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.interceptor.Fault;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.samlidp.SamlPreferences;
import pl.edu.icm.unity.samlidp.SamlProperties;
import pl.edu.icm.unity.samlidp.SamlPreferences.SPSettings;
import pl.edu.icm.unity.samlidp.saml.UnityAttributeQueryValidator;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAttributeQueryContext;
import pl.edu.icm.unity.samlidp.saml.processor.AttributeQueryResponseProcessor;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import xmlbeans.org.oasis.saml2.protocol.AssertionIDRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthzDecisionQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.webservice.SAMLQueryInterface;

/**
 * Implementation of the SAML Assertion Query and Request protocol, SOAP binding.
 * Only attributeQuery is supported as of now.
 * @author K. Benedyczak
 */
public class SAMLAssertionQueryImpl implements SAMLQueryInterface
{
	protected SamlProperties samlProperties;
	protected String endpointAddress;
	protected AttributesManagement attributesMan;
	protected IdentitiesManagement identitiesMan;	
	protected PreferencesManagement preferencesMan;
	
	public SAMLAssertionQueryImpl(SamlProperties samlProperties, String endpointAddress,
			AttributesManagement attributesMan, IdentitiesManagement identitiesMan,
			PreferencesManagement preferencesMan)
	{
		super();
		this.samlProperties = samlProperties;
		this.endpointAddress = endpointAddress;
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.preferencesMan = preferencesMan;
	}

	@Override
	public ResponseDocument attributeQuery(AttributeQueryDocument query)
	{
		SAMLAttributeQueryContext context = new SAMLAttributeQueryContext(query, samlProperties);
		try
		{
			validate(context);
		} catch (SAMLServerException e1)
		{
			throw new Fault(e1);
		}
		AttributeQueryResponseProcessor processor = new AttributeQueryResponseProcessor(context);
		IdentityTaV subjectId = processor.getSubjectsIdentity();
		ResponseDocument respDoc;
		try
		{
			SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan);
			SPSettings spPreferences = preferences.getSPSettings(SamlPreferences.DEFAULT);
			Collection<Attribute<?>> attributes = getAttributes(subjectId, processor, spPreferences);
			respDoc = processor.processAtributeRequest(attributes);
		} catch (Exception e)
		{
			SAMLServerException convertedException = processor.convert2SAMLError(e, null, true);
			respDoc = processor.getErrorResponse(convertedException);
		}
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
	
	protected Collection<Attribute<?>> getAttributes(IdentityTaV subjectId, 
			AttributeQueryResponseProcessor processor, SPSettings preferences) throws EngineException
	{
		EntityParam entity = new EntityParam(subjectId);
		Collection<String> allGroups = identitiesMan.getGroups(entity);
		Collection<AttributeExt<?>> allAttribtues = attributesMan.getAttributes(
				entity, processor.getChosenGroup(), null);
		Map<String, Attribute<?>> all = processor.prepareReleasedAttributes(allAttribtues, allGroups);
		Set<String> hidden = preferences.getHiddenAttribtues();
		for (String hiddenA: hidden)
			all.remove(hiddenA);
		return all.values();
	}

	
	protected void validate(SAMLAttributeQueryContext context) throws SAMLServerException
	{
		UnityAttributeQueryValidator validator = new UnityAttributeQueryValidator(endpointAddress, 
				samlProperties.getSoapTrustChecker(), samlProperties.getRequestValidity(), 
				samlProperties.getReplayChecker());
		
		validator.validate(context.getRequestDocument());
	}

}
