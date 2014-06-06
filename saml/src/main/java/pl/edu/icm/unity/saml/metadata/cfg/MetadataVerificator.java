/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;

import org.w3c.dom.Document;

import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.dsig.DigSignatureUtil;
import eu.unicore.security.dsig.IdAttribute;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;

/**
 * Checks if metadata is trusted and not expired.
 * 
 * @author K. Benedyczak
 */
public class MetadataVerificator
{
	public static final IdAttribute ID_QNAME = new IdAttribute(null, "ID");
	
	public void validate(EntitiesDescriptorDocument metaDoc, MetadataSignatureValidation sigValidation, 
			X509Certificate issuerCertificate) throws MetadataValidationException
	{
		EntitiesDescriptorType meta = metaDoc.getEntitiesDescriptor();
		Date now = new Date();
		validate(meta, now, sigValidation, issuerCertificate);
	}
	
	protected void validate(EntitiesDescriptorType meta, Date now, MetadataSignatureValidation sigValidation, 
			X509Certificate issuerCertificate) throws MetadataValidationException
	{
		if (meta.isSetValidUntil() && meta.getValidUntil().after(now))
			throw new MetadataValidationException("Metadata or its part expired on " + 
					meta.getValidUntil());
		
		if (sigValidation == MetadataSignatureValidation.require)
		{
			validateSignature(issuerCertificate, meta.getName(), (Document) meta.getDomNode());
		}
		
		EntitiesDescriptorType[] nested = meta.getEntitiesDescriptorArray();
		if (nested != null)
		{
			for (EntitiesDescriptorType nestedD: nested)
				validate(nestedD, now, sigValidation, issuerCertificate);
		}
		EntityDescriptorType[] entities = meta.getEntityDescriptorArray();
		
		if (entities != null)
		{
			for (EntityDescriptorType entity: entities)
			{
				validateSingle(entity, now, sigValidation, issuerCertificate);
			}
		}
	}
	
	protected void validateSingle(EntityDescriptorType meta, Date now, MetadataSignatureValidation sigValidation, 
			X509Certificate issuerCertificate) throws MetadataValidationException
	{
		if (meta.isSetValidUntil() && meta.getValidUntil().after(now))
			throw new MetadataValidationException("Metadata or its part expired on " + 
					meta.getValidUntil());
		if (sigValidation == MetadataSignatureValidation.require)
		{
			validateSignature(issuerCertificate, meta.getEntityID(), (Document) meta.getDomNode());
		}
	}

	protected void validateSignature(X509Certificate issuerCertificate, String name, Document doc) 
			throws MetadataValidationException
	{
		try
		{
			DigSignatureUtil sigUtil = new DigSignatureUtil();
			sigUtil.verifyEnvelopedSignature(doc, 
				Collections.singletonList(doc.getDocumentElement()), 
				ID_QNAME, 
				issuerCertificate.getPublicKey());
		} catch (DSigException e)
		{
			throw new MetadataValidationException("Verification of metadata's signature "
					+ "failed for " + name, e);
		}
	}
	
	public static class MetadataValidationException extends Exception
	{
		public MetadataValidationException(String msg)
		{
			super(msg);
		}

		public MetadataValidationException(String msg, Exception cause)
		{
			super(msg, cause);
		}
	}
}
