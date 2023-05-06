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
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import xmlbeans.org.oasis.saml2.metadata.AnyURIListType;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.SPSSODescriptorType;
import xmlbeans.org.w3.x2000.x09.xmldsig.KeyInfoType;

/**
 * Automatically generates SAML metadata from SP configuration.
 * The metadata is never signed - signing must be performed separately.
 * 
 * @author K. Benedyczak
 */
public class SPMetadataGenerator implements MetadataProvider
{
	private Date generationDate;
	private SAMLSPConfiguration samlConfig;
	private EntityDescriptorDocument document;
	private IndexedEndpointType[] assertionConsumerEndpoints;
	private EndpointType[] sloEndpoints;
	
	public SPMetadataGenerator(SAMLSPConfiguration samlConfig, IndexedEndpointType[] assertionConsumerEndpoints, 
			EndpointType[] sloEndpoints)
	{
		this.samlConfig = samlConfig;
		this.assertionConsumerEndpoints = assertionConsumerEndpoints;
		this.sloEndpoints = sloEndpoints;
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
		
		meta.setEntityID(samlConfig.requesterSamlId);

		addSPSSODescriptor(meta);
		
		String asText = document.xmlText(new XmlOptions().setSavePrettyPrint());
		try
		{
			document = EntityDescriptorDocument.Factory.parse(asText);
		} catch (XmlException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void addSPSSODescriptor(EntityDescriptorType meta)
	{
		SPSSODescriptorType spDesc = meta.addNewSPSSODescriptor();
		fillSPDescriptor(spDesc);
		
		spDesc.setAssertionConsumerServiceArray(assertionConsumerEndpoints);
		if (sloEndpoints != null && sloEndpoints.length > 0)
			spDesc.setSingleLogoutServiceArray(sloEndpoints);
	}
	
	private void fillSPDescriptor(SPSSODescriptorType idpDesc)
	{
		AnyURIListType protocolSupport = AnyURIListType.Factory.newInstance();
		protocolSupport.setStringValue(SAMLConstants.PROTOCOL_NS);
		idpDesc.setProtocolSupportEnumeration(protocolSupport.getListValue());
		
		idpDesc.setAuthnRequestsSigned(samlConfig.signRequestByDefault);
		idpDesc.setWantAssertionsSigned(true);

		for (String a: samlConfig.acceptedNameFormats)
			idpDesc.addNameIDFormat(a);
		
		addCredential(samlConfig.requesterCredential, idpDesc);
		addCredential(samlConfig.alternativeRequesterCredential, idpDesc);

	}
	
	private void addCredential(X509Credential issuerCredential, SPSSODescriptorType idpDesc)
	{
		if (issuerCredential != null)
		{
			KeyDescriptorType keyDescriptor = idpDesc.addNewKeyDescriptor();
			KeyInfoType keyInfo = keyDescriptor.addNewKeyInfo();

			X509Certificate cert = issuerCredential.getCertificate();
			XmlBase64Binary xmlCert = keyInfo.addNewX509Data().addNewX509Certificate();
			try
			{
				xmlCert.setByteArrayValue(cert.getEncoded());
			} catch (CertificateEncodingException e)
			{
				throw new RuntimeException("Can not encode SP certificate to binary " +
						"representation for insertion in SAML metadata", e);
			}
		}
	}

	@Override
	public Date getLastmodification()
	{
		return generationDate;
	}

	@Override
	public void stop()
	{
		//nop
	}
}


