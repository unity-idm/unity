/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.proto.AuthnRequest;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.authn.RequestedAuthenticationContextClassReference;
import pl.edu.icm.unity.saml.sp.config.RequestACRsMode;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.NameIDPolicyType;
import xmlbeans.org.oasis.saml2.protocol.RequestedAuthnContextType;

/**
 * Shared SAML utility methods
 * @author K. Benedyczak
 */
public class SAMLHelper
{
	public static AuthnRequestDocument createSAMLRequest(String  responseConsumerAddress,
			boolean sign, String requestrId, String identityProviderURL,
			String requestedNameFormat, boolean allowCreate, X509Credential credential,
			RequestedAuthenticationContextClassReference requestedAuthenticationContextClassReference,
			SAMLSPConfiguration spConfiguration) throws InternalException
	{
		NameID issuer = new NameID(requestrId, SAMLConstants.NFORMAT_ENTITY);
		AuthnRequest request = new AuthnRequest(issuer.getXBean());
		
		if (requestedNameFormat != null)
			request.setFormat(requestedNameFormat);
		if (allowCreate)
		{
			NameIDPolicyType policy = request.getXMLBean().getNameIDPolicy();
			if (policy == null)
				policy = request.getXMLBean().addNewNameIDPolicy();
			policy.setAllowCreate(true);
		}
		if (identityProviderURL != null)
			request.getXMLBean().setDestination(identityProviderURL);
		request.getXMLBean().setAssertionConsumerServiceURL(responseConsumerAddress);
 
		addACRsIfNeeded(request, spConfiguration, requestedAuthenticationContextClassReference);
		
		if (sign)
		{
			try
			{
				request.sign(credential.getKey(), credential.getCertificateChain());
			} catch (Exception e)
			{
				throw new InternalException("Can't sign request", e);
			}
		}
		return request.getXMLBeanDoc();
	}

	private static void addACRsIfNeeded(AuthnRequest request, SAMLSPConfiguration spConfiguration,
			RequestedAuthenticationContextClassReference requestedAuthenticationContextClassReference)
	{
		if (spConfiguration.requestACR.equals(RequestACRsMode.NONE))
		{
			return;
		}

		if (spConfiguration.requestACR.equals(RequestACRsMode.FIXED))
		{
			RequestedAuthnContextType newRequestedAuthnContext = request.getXMLBean()
					.addNewRequestedAuthnContext();
			newRequestedAuthnContext.setAuthnContextClassRefArray(spConfiguration.requestedACRs.toArray(String[]::new));
			newRequestedAuthnContext.setComparison(xmlbeans.org.oasis.saml2.protocol.AuthnContextComparisonType.Enum
					.forString(spConfiguration.comparisonMethod.name()
							.toLowerCase()));
		} else if (spConfiguration.requestACR.equals(RequestACRsMode.FORWARD) && !requestedAuthenticationContextClassReference.getAll().isEmpty())
		{
			RequestedAuthnContextType newRequestedAuthnContext = request.getXMLBean()
					.addNewRequestedAuthnContext();
			newRequestedAuthnContext.setAuthnContextClassRefArray(requestedAuthenticationContextClassReference.getAll()
					.stream()
					.toArray(String[]::new));
			newRequestedAuthnContext.setComparison(xmlbeans.org.oasis.saml2.protocol.AuthnContextComparisonType.EXACT);
		}
	}
}
