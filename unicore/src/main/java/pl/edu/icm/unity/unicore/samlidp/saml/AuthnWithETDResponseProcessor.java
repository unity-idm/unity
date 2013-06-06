/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.saml;

import java.util.Calendar;
import java.util.Collection;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.elements.Subject;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.proto.AssertionResponse;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.etd.DelegationRestrictions;
import eu.unicore.security.etd.ETDApi;
import eu.unicore.security.etd.ETDImpl;
import eu.unicore.security.etd.TrustDelegation;
import pl.edu.icm.unity.samlidp.SamlProperties;
import pl.edu.icm.unity.samlidp.saml.SAMLProcessingException;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.samlidp.saml.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Extension of the {@link AuthnResponseProcessor} which allows for adding a bootstrap ETD
 * assertion to the response.
 *  
 * @author K. Benedyczak
 */
public class AuthnWithETDResponseProcessor extends AuthnResponseProcessor
{
	public AuthnWithETDResponseProcessor(SAMLAuthnContext context)
	{
		super(context);
	}
	
	public AuthnWithETDResponseProcessor(SAMLAuthnContext context, Calendar authnTime)
	{
		super(context, authnTime);
	}


	public ResponseDocument processAuthnRequest(Identity authenticatedIdentity, Collection<Attribute<?>> attributes,
			DelegationRestrictions restrictions) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		Subject authenticatedOne = establishSubject(authenticatedIdentity);

		AssertionResponse resp = getOKResponseDocument();
		resp.addAssertion(createAuthenticationAssertion(authenticatedOne));
		if (attributes != null)
		{
			Assertion assertion = createAttributeAssertion(authenticatedOne, attributes);
			if (assertion != null)
				resp.addAssertion(assertion);
		}
		if (restrictions != null)
		{
			Assertion assertion = generateTD(authenticatedIdentity, restrictions);
			resp.addAssertion(assertion);
		}
		return resp.getXMLBeanDoc();
	}
	
	protected TrustDelegation generateTD(Identity custodian, DelegationRestrictions restrictions) 
			throws SAMLProcessingException
	{
		ETDApi etdEngine = new ETDImpl();
		X509Credential issuerCredential = samlConfiguration.getSamlIssuerCredential(); 
		String issuerName = samlConfiguration.getValue(SamlProperties.ISSUER_URI); 
		String custodianDN = X500NameUtils.getPortableRFC2253Form(custodian.getValue());
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
}
