/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.elements.Subject;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.proto.AssertionResponse;
import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.slo.SamlRoutableSignableMessage;
import xmlbeans.org.oasis.saml2.assertion.AuthnContextType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;
import xmlbeans.org.oasis.saml2.protocol.NameIDPolicyType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import java.time.Instant;
import java.util.*;

import static pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.AssertionSigningPolicy;

/**
 * Extends {@link StatusResponseProcessor} to produce SAML Response documents,
 * which are returned in the Authentication Request and Assertion Query
 * protocols.
 * 
 * @author K. Benedyczak
 */
public class AuthnResponseProcessor extends BaseResponseProcessor<AuthnRequestDocument, AuthnRequestType>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AuthnResponseProcessor.class);
	private String sessionId;
	private SubjectType authenticatedSubject;
	private LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;

	public AuthnResponseProcessor(AttributeTypeSupport aTypeSupport,
			LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement, SAMLAuthnContext context)
	{
		this(aTypeSupport, lastAccessAttributeManagement ,context, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
	}

	public AuthnResponseProcessor(AttributeTypeSupport aTypeSupport, LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement,
			SAMLAuthnContext context, Calendar authnTime)
	{
		super(aTypeSupport, context, authnTime);
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
	}

	public List<IdentityParam> getCompatibleIdentities(Collection<? extends IdentityParam> identities)
			throws SAMLRequesterException
	{
		String samlFormat = getRequestedFormat();
		String unityFormat = samlConfiguration.idTypeMapper.mapIdentity(samlFormat);
		List<IdentityParam> ret = new ArrayList<>();
		for (IdentityParam identity : identities)
		{
			if (identity.getTypeId().equals(unityFormat))
				ret.add(identity);
		}
		log.debug("Requested identity {}, mapped to {}, returning identities: {}", samlFormat, unityFormat, ret);
		if (ret.size() > 0)
			return ret;
		throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_UNKNOWN_PRINCIPAL,
				"There is no identity of the requested '" + samlFormat
						+ "' SAML identity format for the authenticated principial.");
	}

	public boolean isIdentityCreationAllowed()
	{
		NameIDPolicyType nameIdPolicy = context.getRequest().getNameIDPolicy();
		if (nameIdPolicy == null)
			return true;
		return nameIdPolicy.getAllowCreate();
	}

	public SamlRoutableSignableMessage<ResponseDocument> processAuthnRequestReturningResponse(
			IdentityParam authenticatedIdentity, Collection<Attribute> attributes, String responseRelayState,
			String responseDestination) throws SAMLRequesterException, SAMLProcessingException
	{
		boolean returnSingleAssertion = samlConfiguration.returnSingleAssertion;
		return processAuthnRequest(authenticatedIdentity, attributes, returnSingleAssertion, responseRelayState,
				responseDestination);
	}

	protected SamlRoutableSignableMessage<ResponseDocument> processAuthnRequest(IdentityParam authenticatedIdentity,
			Collection<Attribute> attributes, boolean returnSingleAssertion, String relayState, String destination)
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

		X509Credential responseSigningKey = null;
		if (doSignResponse())
		{
			if (destination == null)
				throw new SAMLProcessingException("Unable to determine Destination "
						+ "value which is mandatory when signing response is requested");
			responseSigningKey = samlConfiguration.getSamlIssuerCredential();
		}
		
		try
		{
			lastAccessAttributeManagement.setAttribute(new EntityParam(authenticatedIdentity),
					AccessProtocol.SAML, new ApplicationId(context.getRequest().getIssuer().getStringValue()), Instant.now());
		} catch (EngineException e)
		{
			log.error("Can not set last access attribute", e);
		}
		
		return new SamlRoutableSignableMessage<>(resp, responseSigningKey, SAMLMessageType.SAMLResponse, relayState,
				destination);
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
		validity.setTimeInMillis(getAuthnTime().getTimeInMillis() + samlConfiguration.requestValidityPeriod.toMillis());
		confData.setNotOnOrAfter(validity);
		String consumerServiceURL = samlConfiguration.getReturnAddressForRequester(context.getRequest());
		confData.setRecipient(consumerServiceURL);
		requested.setSubjectConfirmationArray(new SubjectConfirmationType[]
		{ subConf });
	}

	protected Assertion createAuthenticationAssertion(SubjectType authenticatedOne, Collection<Attribute> attributes)
			throws SAMLProcessingException
	{
		this.authenticatedSubject = authenticatedOne;
		AuthnContextType authContext = setupAuthnContext();
		Assertion assertion = new Assertion();
		assertion.setIssuer(samlConfiguration.issuerURI, SAMLConstants.NFORMAT_ENTITY);
		assertion.setSubject(authenticatedOne);
		this.sessionId = assertion.getXMLBean().getID();
		assertion.addAuthStatement(getAuthnTime(), authContext, sessionId, null, null);
		assertion.setAudienceRestriction(new String[]
		{ context.getRequest().getIssuer().getStringValue() });

		if (attributes != null)
			addAttributesToAssertion(assertion, attributes);

		AssertionSigningPolicy assertionSigningPolicy = samlConfiguration.signAssertion;
		if (assertionSigningPolicy == AssertionSigningPolicy.always || !doSignResponse())
			signAssertion(assertion);
		return assertion;
	}

	/**
	 * Only unspecified - it's too much work to implement it fully with minimal
	 * effect.
	 * 
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
	 * @return assigned session ID. Note that it will return null until the final
	 *         authN assertion is produced.
	 */
	public String getSessionId()
	{
		return sessionId;
	}

	/**
	 * @return returned user's ID. Note that it will return null until the final
	 *         authN assertion is produced.
	 */
	public SubjectType getAuthenticatedSubject()
	{
		return authenticatedSubject;
	}
}
