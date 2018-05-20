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
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties.AssertionSigningPolicy;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
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
	private String sessionId;
	private SubjectType authenticatedSubject;
	
	public AuthnResponseProcessor(AttributeTypeSupport aTypeSupport, SAMLAuthnContext context)
	{
		this(aTypeSupport, context, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
	}
	
	public AuthnResponseProcessor(AttributeTypeSupport aTypeSupport, SAMLAuthnContext context, Calendar authnTime)
	{
		super(aTypeSupport, context, authnTime);
	}

	public List<IdentityParam> getCompatibleIdentities(Collection<? extends IdentityParam> identities) 
			throws SAMLRequesterException
	{
		String samlFormat = getRequestedFormat();
		String unityFormat = samlConfiguration.getIdTypeMapper().mapIdentity(samlFormat);
		List<IdentityParam> ret = new ArrayList<>();
		for (IdentityParam identity: identities)
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
	
	public ResponseDocument processAuthnRequest(Identity authenticatedIdentity, String destination) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		return processAuthnRequest(authenticatedIdentity, null, destination);
	}
	
	public ResponseDocument processAuthnRequest(IdentityParam authenticatedIdentity, 
			Collection<Attribute> attributes, String destination) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		boolean returnSingleAssertion = samlConfiguration.getBooleanValue(
				SamlIdpProperties.RETURN_SINGLE_ASSERTION);
		return processAuthnRequest(authenticatedIdentity, attributes, returnSingleAssertion,
				destination);
	}
	
	protected ResponseDocument processAuthnRequest(IdentityParam authenticatedIdentity, 
			Collection<Attribute> attributes, boolean returnSingleAssertion, String destination) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		SubjectType authenticatedOne = establishSubject(authenticatedIdentity);

		AssertionResponse resp = getOKResponseDocument();
		if (destination != null)
			resp.getXMLBean().setDestination(destination);
		if (returnSingleAssertion)
		{
			Assertion authnAssertion = createAuthenticationAssertion(authenticatedOne, attributes);
			addAssertionEncrypting(resp, authnAssertion);
		} else
		{
			Assertion authnAssertion = createAuthenticationAssertion(authenticatedOne, null);
			addAssertionEncrypting(resp, authnAssertion);
			if (attributes != null)
			{
				SubjectType attributeAssertionSubject = cloneSubject(authenticatedOne);
				setSenderVouchesSubjectConfirmation(attributeAssertionSubject);
				Assertion assertion = createAttributeAssertion(attributeAssertionSubject, attributes);
				if (assertion != null)
					addAssertionEncrypting(resp, assertion);
			}
		}
		
		if (doSignResponse())
		{
			if (destination == null)
				throw new SAMLProcessingException("Unable to determine Destination "
						+ "value which is mandatory when signing response is requested");
			signResponse(resp);
		}
		return resp.getXMLBeanDoc();
	}
	
	protected SubjectType establishSubject(IdentityParam authenticatedIdentity)
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
		String consumerServiceURL = samlConfiguration.getReturnAddressForRequester(
					context.getRequest());
		confData.setRecipient(consumerServiceURL);
		requested.setSubjectConfirmationArray(new SubjectConfirmationType[] {subConf});
	}

	protected Assertion createAuthenticationAssertion(SubjectType authenticatedOne, 
			Collection<Attribute> attributes) throws SAMLProcessingException
	{
		this.authenticatedSubject = authenticatedOne;
		AuthnContextType authContext = setupAuthnContext();
		Assertion assertion = new Assertion();
		assertion.setIssuer(samlConfiguration.getValue(SamlIdpProperties.ISSUER_URI), 
				SAMLConstants.NFORMAT_ENTITY);
		assertion.setSubject(authenticatedOne);
		this.sessionId = assertion.getXMLBean().getID();
		assertion.addAuthStatement(getAuthnTime(), authContext, sessionId, null, null);
		assertion.setAudienceRestriction(new String[] {context.getRequest().getIssuer().getStringValue()});

		if (attributes != null)
			addAttributesToAssertion(assertion, attributes);
		
		AssertionSigningPolicy assertionSigningPolicy = 
				samlConfiguration.getEnumValue(SamlIdpProperties.SIGN_ASSERTION, 
						AssertionSigningPolicy.class);
		if (assertionSigningPolicy == AssertionSigningPolicy.always || !doSignResponse())
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
		
	protected Subject convertIdentity(IdentityParam unityIdentity, String requestedSamlFormat)
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

	/**
	 * @return assigned session ID. Note that it will return null until the final authN assertion is produced.
	 */
	public String getSessionId()
	{
		return sessionId;
	}

	/**
	 * @return returned user's ID. Note that it will return null until the final authN assertion is produced.
	 */
	public SubjectType getAuthenticatedSubject()
	{
		return authenticatedSubject;
	}
}
