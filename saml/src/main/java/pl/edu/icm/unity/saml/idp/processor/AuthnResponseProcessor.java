/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.elements.Subject;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.proto.AssertionResponse;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import xmlbeans.org.oasis.saml2.assertion.AuthnContextType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
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
	public AuthnResponseProcessor(SAMLAuthnContext context)
	{
		this(context, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
	}
	
	public AuthnResponseProcessor(SAMLAuthnContext context, Calendar authnTime)
	{
		super(context, authnTime);
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
	
	public boolean isIdentityCreationAllowed()
	{
		NameIDPolicyType nameIdPolicy = context.getRequest().getNameIDPolicy();
		if (nameIdPolicy == null)
			return true;
		return nameIdPolicy.getAllowCreate();
	}
	
	public String getIdentityTarget()
	{
		return context.getRequest().getIssuer().getStringValue();
	}
	
	public ResponseDocument processAuthnRequest(Identity authenticatedIdentity) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		return processAuthnRequest(authenticatedIdentity, null);
	}
	
	public ResponseDocument processAuthnRequest(Identity authenticatedIdentity, Collection<Attribute<?>> attributes) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		boolean returnSingleAssertion = samlConfiguration.getBooleanValue(
				SamlIdpProperties.RETURN_SINGLE_ASSERTION);
		return processAuthnRequest(authenticatedIdentity, attributes, returnSingleAssertion);
	}
	
	protected ResponseDocument processAuthnRequest(Identity authenticatedIdentity, 
			Collection<Attribute<?>> attributes, boolean returnSingleAssertion) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		SubjectType authenticatedOne = establishSubject(authenticatedIdentity);

		AssertionResponse resp = getOKResponseDocument();
		
		if (returnSingleAssertion)
			resp.addAssertion(createAuthenticationAssertion(authenticatedOne, attributes));
		else
			resp.addAssertion(createAuthenticationAssertion(authenticatedOne, null));
		
		if (attributes != null && !returnSingleAssertion)
		{
			SubjectType attributeAssertionSubject = cloneSubject(authenticatedOne);
			setSenderVouchesSubjectConfirmation(attributeAssertionSubject);
			Assertion assertion = createAttributeAssertion(attributeAssertionSubject, attributes);
			if (assertion != null)
				resp.addAssertion(assertion);
		}
		return resp.getXMLBeanDoc();
	}
	
	protected SubjectType establishSubject(Identity authenticatedIdentity)
	{
		String format = getRequestedFormat();
		Subject authenticatedOne = convertIdentity(authenticatedIdentity, format);
		
		SubjectType ret = authenticatedOne.getXBean();
		setBearerSubjectConfirmation(ret);
		return ret;
	}

	protected void setBearerSubjectConfirmation(SubjectType requested)
	{
		SubjectConfirmationType subConf = SubjectConfirmationType.Factory.newInstance();
		subConf.setMethod(SAMLConstants.CONFIRMATION_BEARER);
		SubjectConfirmationDataType confData = subConf.addNewSubjectConfirmationData();
		confData.setInResponseTo(context.getRequest().getID());
		Calendar validity = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		validity.setTimeInMillis(getAuthnTime().getTimeInMillis()+samlConfiguration.getRequestValidity());
		confData.setNotOnOrAfter(validity);
		String consumerServiceURL = context.getRequest().getAssertionConsumerServiceURL();
		if (consumerServiceURL == null)
			consumerServiceURL = samlConfiguration.getReturnAddressForRequester(
					context.getRequest().getIssuer());
		confData.setRecipient(consumerServiceURL);
		requested.setSubjectConfirmationArray(new SubjectConfirmationType[] {subConf});
	}

	protected Assertion createAuthenticationAssertion(SubjectType authenticatedOne, 
			Collection<Attribute<?>> attributes) throws SAMLProcessingException
	{
		AuthnContextType authContext = setupAuthnContext();
		Assertion assertion = new Assertion();
		assertion.setIssuer(samlConfiguration.getValue(SamlIdpProperties.ISSUER_URI), 
				SAMLConstants.NFORMAT_ENTITY);
		assertion.setSubject(authenticatedOne);
		assertion.addAuthStatement(getAuthnTime(), authContext);
		assertion.setAudienceRestriction(new String[] {context.getRequest().getIssuer().getStringValue()});

		if (attributes != null)
			addAttributesToAssertion(assertion, attributes);
		
		signAssertion(assertion);
		return assertion;
	}
	
	/**
	 * Only unspecified - it's too much work to implement it fully
	 * with minimal effect.
	 * @return
	 */
	protected AuthnContextType setupAuthnContext()
	{
		AuthnContextType ret = AuthnContextType.Factory.newInstance();
		ret.setAuthnContextClassRef(SAMLConstants.SAML_AC_UNSPEC);
		return ret;
	}
		
	protected Subject convertIdentity(Identity unityIdentity, String requestedSamlFormat)
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
	
	protected String getUnityIdentityFormat(String samlIdFormat) throws SAMLRequesterException
	{
		if (samlIdFormat.equals(SAMLConstants.NFORMAT_PERSISTENT) || 
				samlIdFormat.equals(SAMLConstants.NFORMAT_UNSPEC))
		{
			return TargetedPersistentIdentity.ID;
		} else if (samlIdFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			return X500Identity.ID;
		} else if (samlIdFormat.equals(SAMLConstants.NFORMAT_TRANSIENT))
		{
			return TransientIdentity.ID;
		} else
		{
			throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_INVALID_NAMEID_POLICY,
					samlIdFormat + " is not supported by this service.");
		}
	}
}
