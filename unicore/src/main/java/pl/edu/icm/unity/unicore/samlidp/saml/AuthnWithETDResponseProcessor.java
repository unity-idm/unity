/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.saml;

import java.util.Calendar;
import java.util.Collection;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.proto.AssertionResponse;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.etd.DelegationRestrictions;
import eu.unicore.security.etd.ETDApi;
import eu.unicore.security.etd.ETDImpl;
import eu.unicore.security.etd.TrustDelegation;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Extension of the {@link AuthnResponseProcessor} which allows for adding a bootstrap ETD
 * assertion to the response.
 *  
 * @author K. Benedyczak
 */
public class AuthnWithETDResponseProcessor extends AuthnResponseProcessor
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, AuthnWithETDResponseProcessor.class);
	
	public AuthnWithETDResponseProcessor(AttributeTypeSupport aTypeSupport, SAMLAuthnContext context)
	{
		super(aTypeSupport, context);
	}
	
	public AuthnWithETDResponseProcessor(AttributeTypeSupport aTypeSupport, SAMLAuthnContext context, 
			Calendar authnTime)
	{
		super(aTypeSupport, context, authnTime);
	}


	public ResponseDocument processAuthnRequest(IdentityParam authenticatedIdentity, 
			Collection<Attribute> attributes,
			String destination,
			DelegationRestrictions restrictions) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		if (samlConfiguration.getBooleanValue(SamlIdpProperties.RETURN_SINGLE_ASSERTION))
			log.info("The " + SamlIdpProperties.RETURN_SINGLE_ASSERTION + 
					" = true setting is ignored for UNICORE IdP. " +
					"Set it to false to disable this message");
		
		boolean etdMode = checkX500Issuer(getContext().getRequest().getIssuer()) && 
				SAMLConstants.NFORMAT_DN.equals(getRequestedFormat());
		if (!etdMode)
			return super.processAuthnRequest(authenticatedIdentity, attributes, false,
					destination);
		
		SubjectType authenticatedOne = establishSubject(authenticatedIdentity);

		AssertionResponse resp = getOKResponseDocument();
		resp.addAssertion(createAuthenticationAssertion(authenticatedOne, null));

		if (attributes != null)
		{
			SubjectType attributeAssertionSubject = cloneSubject(authenticatedOne);
			setSenderVouchesSubjectConfirmation(attributeAssertionSubject);
			Assertion assertion = createAttributeAssertion(attributeAssertionSubject, attributes);
			if (assertion != null)
				resp.addAssertion(assertion);
		}
		if (restrictions != null)
		{
			Assertion assertion = generateTD(authenticatedOne.getNameID().getStringValue(), restrictions);
			resp.addAssertion(assertion);
		}
		return resp.getXMLBeanDoc();
	}
	
	protected TrustDelegation generateTD(String custodian, DelegationRestrictions restrictions) 
			throws SAMLProcessingException
	{
		ETDApi etdEngine = new ETDImpl();
		X509Credential issuerCredential = samlConfiguration.getSamlIssuerCredential(); 
		String issuerName = samlConfiguration.getValue(SamlIdpProperties.ISSUER_URI); 
		String custodianDN = X500NameUtils.getPortableRFC2253Form(custodian);
		NameIDType receiver = context.getRequest().getIssuer();
		String receiverDN = X500NameUtils.getPortableRFC2253Form(receiver.getStringValue());
		try
		{
			return etdEngine.generateBootstrapTD(custodianDN, issuerCredential.getCertificateChain(),
					issuerName, SAMLConstants.NFORMAT_ENTITY,
					issuerCredential.getKey(), receiverDN, restrictions);
		} catch (DSigException e)
		{
			throw new SAMLProcessingException("Internal error while signing the trust delegation", e);
		}
	}
	
	
	protected boolean checkX500Issuer(NameIDType issuer)
	{
		if (issuer == null)
			return false;
		if (issuer.getFormat() == null || !issuer.getFormat().equals(SAMLConstants.NFORMAT_DN))
			return false;
		if (issuer.getStringValue() == null)
			return false;
		try
		{
			X500NameUtils.getX500Principal(issuer.getStringValue());
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}
}
