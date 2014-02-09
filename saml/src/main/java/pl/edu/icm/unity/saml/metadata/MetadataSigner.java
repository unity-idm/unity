/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.dsig.DigSignatureUtil;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;


/**
 * Decorator: takes a {@link MetadataProvider} and signs the content. The ID attribute is also generated.
 * @author K. Benedyczak
 */
public class MetadataSigner implements MetadataProvider
{
	private EntityDescriptorDocument metadata;
	
	public MetadataSigner(MetadataProvider wrappedProvider, X509Credential credential) throws DSigException
	{
		metadata = wrappedProvider.getMetadata();
		
		String id = SAMLUtils.genID("unity-");
		metadata.getEntityDescriptor().setID(id);
		Document docToSign = (Document) metadata.getDomNode();
		DigSignatureUtil signer = new DigSignatureUtil();
		signer.genEnvelopedSignature(credential.getKey(), credential.getCertificate().getPublicKey(), 
				credential.getCertificateChain(), docToSign, docToSign.getFirstChild().getFirstChild(), 
				SamlTrustChecker.PROTOCOL_ID_QNAME);
	}

	@Override
	public EntityDescriptorDocument getMetadata()
	{
		try
		{
			return EntityDescriptorDocument.Factory.parse(metadata.xmlText());
		} catch (XmlException e)
		{
			throw new RuntimeException("Can't re-parse metadata?", e);
		}
	}
}
