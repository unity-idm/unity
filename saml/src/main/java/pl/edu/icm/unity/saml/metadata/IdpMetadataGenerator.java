/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.apache.xmlbeans.XmlBase64Binary;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties.RequestAcceptancePolicy;
import xmlbeans.org.oasis.saml2.metadata.AnyURIListType;
import xmlbeans.org.oasis.saml2.metadata.AttributeAuthorityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.IDPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyTypes;
import xmlbeans.org.oasis.saml2.metadata.RoleDescriptorType;
import xmlbeans.org.w3.x2000.x09.xmldsig.KeyInfoType;

/**
 * Automatically generates SAML metadata from IdP configuration.
 * The metadata is never signed - signing must be performed separately.
 * 
 * @author K. Benedyczak
 */
public class IdpMetadataGenerator implements MetadataProvider
{
	private Date generationDate;
	private SamlIdpProperties samlConfig;
	private EntityDescriptorDocument document;
	private EndpointType[] ssoEndpoints;
	private EndpointType[] attributeQueryEndpoints;
	
	public IdpMetadataGenerator(SamlIdpProperties samlConfig, EndpointType[] ssoEndpoints, 
			EndpointType[] attributeQueryEndpoints)
	{
		this.samlConfig = samlConfig;
		this.ssoEndpoints = ssoEndpoints;
		this.attributeQueryEndpoints = attributeQueryEndpoints;
		generateMetadata();
	}

	@Override
	public EntityDescriptorDocument getMetadata()
	{
		try
		{
			return EntityDescriptorDocument.Factory.parse(document.xmlText());
		} catch (XmlException e)
		{
			throw new RuntimeException("Can't re-parse metadata?", e);
		}
	}
	
	private void generateMetadata()
	{
		generationDate = new Date();
		document = EntityDescriptorDocument.Factory.newInstance(new XmlOptions().setSavePrettyPrint());
		
		EntityDescriptorType meta = document.addNewEntityDescriptor();
		
		meta.setEntityID(samlConfig.getValue(SamlIdpProperties.ISSUER_URI));

		if (ssoEndpoints != null && ssoEndpoints.length > 0)
			addIdpSSODescriptor(meta);
		if (attributeQueryEndpoints != null && attributeQueryEndpoints.length > 0)
			addIdpAttributeAuthorityDescriptor(meta);
		
		String asText = document.xmlText(new XmlOptions().setSavePrettyPrint());
		try
		{
			document = EntityDescriptorDocument.Factory.parse(asText);
		} catch (XmlException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void addIdpSSODescriptor(EntityDescriptorType meta)
	{
		IDPSSODescriptorType idpDesc = meta.addNewIDPSSODescriptor();
		fillIdpGenericDescriptor(idpDesc);

		RequestAcceptancePolicy acceptancePolicy = samlConfig.getEnumValue(SamlIdpProperties.SP_ACCEPT_POLICY, 
				RequestAcceptancePolicy.class);
		idpDesc.setWantAuthnRequestsSigned(acceptancePolicy == RequestAcceptancePolicy.strict ||
				acceptancePolicy == RequestAcceptancePolicy.validSigner);
		
		idpDesc.setSingleSignOnServiceArray(ssoEndpoints);
	}
	
	private void addIdpAttributeAuthorityDescriptor(EntityDescriptorType meta)
	{
		AttributeAuthorityDescriptorType idpDesc = meta.addNewAttributeAuthorityDescriptor();
		fillIdpGenericDescriptor(idpDesc);
		idpDesc.setAttributeServiceArray(attributeQueryEndpoints);
	}
	
	private void fillIdpGenericDescriptor(RoleDescriptorType idpDesc)
	{
		AnyURIListType protocolSupport = AnyURIListType.Factory.newInstance();
		protocolSupport.setStringValue(SAMLConstants.PROTOCOL_NS);
		idpDesc.setProtocolSupportEnumeration(protocolSupport.getListValue());
		
		KeyDescriptorType keyDescriptor = idpDesc.addNewKeyDescriptor();
		KeyInfoType keyInfo = keyDescriptor.addNewKeyInfo();
		X509Credential issuerCredential = samlConfig.getSamlIssuerCredential();
		X509Certificate cert = issuerCredential.getCertificate();
		XmlBase64Binary xmlCert = keyInfo.addNewX509Data().addNewX509Certificate();
		try
		{
			xmlCert.setByteArrayValue(cert.getEncoded());
		} catch (CertificateEncodingException e)
		{
			throw new RuntimeException("Can not encode IdP certificate to binary " +
					"representation for insertion in SAML metadata", e);
		}
		keyDescriptor.setUse(KeyTypes.SIGNING);
	}

	@Override
	public Date getLastmodification()
	{
		return generationDate;
	}
}


