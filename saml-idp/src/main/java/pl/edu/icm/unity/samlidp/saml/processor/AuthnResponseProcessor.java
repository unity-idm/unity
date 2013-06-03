/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml.processor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.elements.Subject;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.proto.AssertionResponse;
import pl.edu.icm.unity.samlidp.SamlProperties;
import pl.edu.icm.unity.samlidp.saml.SAMLProcessingException;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import xmlbeans.org.oasis.saml2.assertion.AuthnContextType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;
import xmlbeans.org.oasis.saml2.protocol.NameIDPolicyType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Extends {@link StatusResponseProcessor} to produce SAML Response documents, 
 * which are returned in the Authentication Request and Assertion Query protocols. 
 * @author K. Benedyczak
 */
public class AuthnResponseProcessor extends BaseResponseProcessor<AuthnRequestDocument, AuthnRequestType>
{
	private Calendar authnTime;

	public AuthnResponseProcessor(SAMLAuthnContext context)
	{
		this(context, null);
	}
	
	public AuthnResponseProcessor(SAMLAuthnContext context, Calendar authnTime)
	{
		super(context);
		this.authnTime = authnTime;
	}

	public List<Identity> getCompatibleIdentities(Entity authenticatedEntity) 
			throws SAMLRequesterException
	{
		String samlFormat = getRequestedFormat();
		Identity[] identities = authenticatedEntity.getIdentities();
		String unityFormat = getUnityIdentityFormat(samlFormat);
		List<Identity> ret = new ArrayList<Identity>();
		for (Identity identity: identities)
		{
			if (identity.getTypeId().equals(unityFormat))
				ret.add(identity);
		}
		if (ret.size() > 0)
			return ret;
		throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_UNKNOWN_PRINCIPIAL,
				"There is no identity of the requested '" + samlFormat + 
				"' SAML identity format for the authenticated principial.");			
	}
	
	public ResponseDocument processAuthnRequest(Identity authenticatedIdentity) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		return processAuthnRequest(authenticatedIdentity, null);
	}
	
	public ResponseDocument processAuthnRequest(Identity authenticatedIdentity, Collection<Attribute<?>> attributes) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		SamlProperties samlConfiguration = context.getSamlConfiguration();

		String format = getRequestedFormat();
		Subject authenticatedOne = convertIdentity(authenticatedIdentity, format);
		
		SubjectConfirmationType subConf = SubjectConfirmationType.Factory.newInstance();
		subConf.setMethod(SAMLConstants.CONFIRMATION_BEARER);
		SubjectConfirmationDataType confData = subConf.addNewSubjectConfirmationData();
		confData.setInResponseTo(context.getRequest().getID());
		confData.setRecipient(context.getRequest().getAssertionConsumerServiceURL());
		Calendar validity = Calendar.getInstance();
		validity.setTimeInMillis(authnTime.getTimeInMillis()+samlConfiguration.getRequestValidity());
		confData.setNotOnOrAfter(validity);
		authenticatedOne.setSubjectConfirmation(
				new SubjectConfirmationType[] {subConf});
				
		AssertionResponse resp = getOKResponseDocument();
		resp.addAssertion(createAuthenticationAssertion(authenticatedOne));
		if (attributes != null)
			resp.addAssertion(createAttributeAssertion(authenticatedOne, attributes));
		return resp.getXMLBeanDoc();
	}

	private Assertion createAuthenticationAssertion(Subject authenticatedOne) throws SAMLProcessingException
	{
		AuthnContextType authContext = setupAuthnContext();
		Assertion assertion = new Assertion();
		assertion.setIssuer(samlConfiguration.getValue(SamlProperties.ISSUER_URI), 
				SAMLConstants.NFORMAT_ENTITY);
		assertion.setSubject(authenticatedOne.getXBean());
		assertion.addAuthStatement(authnTime, authContext);
		assertion.setAudienceRestriction(new String[] {context.getRequest().getIssuer().getStringValue()});

		signAssertion(assertion);
		return assertion;
	}
	
	/**
	 * Only unspecified - it's too much work to implement it fully
	 * with minimal effect.
	 * @return
	 */
	private AuthnContextType setupAuthnContext()
	{
		AuthnContextType ret = AuthnContextType.Factory.newInstance();
		ret.setAuthnContextClassRef(SAMLConstants.SAML_AC_UNSPEC);
		return ret;
	}
		
	private Subject convertIdentity(Identity unityIdentity, String requestedSamlFormat)
	{
		if (requestedSamlFormat.equals(SAMLConstants.NFORMAT_UNSPEC))
			requestedSamlFormat = SAMLConstants.NFORMAT_PERSISTENT;
		return new Subject(unityIdentity.getValue(), requestedSamlFormat);
	}

	protected String getRequestedFormat()
	{
		String requestedFormat = null;
		NameIDPolicyType nameIDPolicy = getContext().getRequest().getNameIDPolicy();
		if (nameIDPolicy != null)
			requestedFormat = nameIDPolicy.getFormat();
		if (requestedFormat == null)
			return SAMLConstants.NFORMAT_UNSPEC;
		return requestedFormat;
	}
	
	private String getUnityIdentityFormat(String samlIdFormat) throws SAMLRequesterException
	{
		if (samlIdFormat.equals(SAMLConstants.NFORMAT_PERSISTENT) || 
				samlIdFormat.equals(SAMLConstants.NFORMAT_UNSPEC))
		{
			return PersistentIdentity.ID;
		} else if (samlIdFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			return X500Identity.ID;
		} else
		{
			throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_INVALID_NAMEID_POLICY,
					samlIdFormat + " is not supported by this service.");
		}
	}
}
