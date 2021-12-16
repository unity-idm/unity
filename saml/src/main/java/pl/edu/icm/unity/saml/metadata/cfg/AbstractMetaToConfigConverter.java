/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.ExtensionsType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyTypes;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesDocument;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesType;
import xmlbeans.org.w3.x2000.x09.xmldsig.X509DataType;

/**
 * Base for converters of SAML metadata into a series of property statements.
 *  
 * @author K. Benedyczak
 */
public abstract class AbstractMetaToConfigConverter
{
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AbstractMetaToConfigConverter.class);
	protected PKIManagement pkiManagement;
	protected MessageSource msg;
	
	public AbstractMetaToConfigConverter(PKIManagement pkiManagement, MessageSource msg)
	{
		this.pkiManagement = pkiManagement;
		this.msg = msg;
	}

	/**
	 * Inserts metadata to the configuration in properties. All entries which are present in 
	 * realConfig are preserved. 
	 * @param meta
	 * @param properties
	 * @param realConfig
	 */
	protected void convertToProperties(EntitiesDescriptorDocument metaDoc, Properties properties, 
			SamlProperties realConfig, String configKey)
	{
		EntitiesDescriptorType meta = metaDoc.getEntitiesDescriptor();
		convertToProperties(meta, properties, realConfig, configKey);
	}
	
	protected void convertToProperties(EntitiesDescriptorType meta, Properties properties, 
			SamlProperties realConfig, String configKey)
	{
		EntitiesDescriptorType[] nested = meta.getEntitiesDescriptorArray();
		if (nested != null)
		{
			for (EntitiesDescriptorType nestedD: nested)
				convertToProperties(nestedD, properties, realConfig, configKey);
		}
		EntityDescriptorType[] entities = meta.getEntityDescriptorArray();
		
		if (entities != null)
		{
			for (EntityDescriptorType entity: entities)
			{
				convertToProperties(meta, entity, properties, realConfig, configKey);
			}
		}
	}
	
	protected abstract void convertToProperties(EntitiesDescriptorType parentMeta, EntityDescriptorType meta, Properties properties, 
			SamlProperties realConfig, String configKey);
	
	/**
	 * 
	 * @param entityAttributes
	 * @return true only if the entity attribtues parameter is not null and contains an REFEDS attribute
	 * hide-from-discovery. 
	 */
	protected boolean isDisabled(EntityAttributesType entityAttributes)
	{
		if (entityAttributes == null)
			return false;
		AttributeType[] attributeArray = entityAttributes.getAttributeArray();
		for (AttributeType a: attributeArray)
		{
			if ("http://macedir.org/entity-category".equals(a.getName()))
			{
				for (XmlObject value : a.getAttributeValueArray())
				{
					XmlCursor c = value.newCursor();
					String valueStr = c.getTextValue();
					c.dispose();
					if (valueStr.equals("http://refeds.org/category/hide-from-discovery"))
						return true;
				}
			}
		}
		return false;
	}
	
	protected List<X509Certificate> getSigningCerts(KeyDescriptorType[] keys, String entityId)
	{
		List<X509Certificate> ret = new ArrayList<X509Certificate>();
		for (KeyDescriptorType key: keys)
		{
			if (!key.isSetUse() || KeyTypes.SIGNING.equals(key.getUse()))
			{
				X509DataType[] x509Keys = key.getKeyInfo().getX509DataArray();
				if (x509Keys == null || x509Keys.length == 0)
				{
					log.info("Key in SAML metadata is ignored as it doesn't contain "
							+ "X.509 certificate. Entity " + entityId);
					continue;
				}
				for (X509DataType x509Key: x509Keys)
				{
					byte[][] certsAsBytes = x509Key.getX509CertificateArray();
					X509Certificate cert;
					try
					{
						cert = CertificateUtils.loadCertificate(
								new ByteArrayInputStream(certsAsBytes[0]), Encoding.DER);
					} catch (IOException e)
					{
						log.warn("Can not load/parse a certificate from metadata of " + entityId
								+ ", ignoring it", e);
						continue;
					}
					ret.add(cert);
				}
			}
		}
		return ret;
	}
		
	protected void updatePKICerts(List<X509Certificate> certs, String entityId, String prefix)
			throws EngineException
	{
		synchronized (pkiManagement)
		{
			for (X509Certificate cert : certs)
			{
				String pkiKey = getCertificateKey(cert, entityId, prefix);
				try
				{
					X509Certificate existingCert = pkiManagement.getCertificate(pkiKey).value;
					if (!existingCert.equals(cert))
					{
						pkiManagement.updateCertificate(new NamedCertificate(pkiKey, cert));
						log.debug("Updated already installed certificate of SAML entity {}, DN: {}, serial: {}",
								entityId, cert.getSubjectX500Principal().getName(), 
								cert.getSerialNumber());
					}
				} catch (IllegalArgumentException e)
				{
					pkiManagement.addVolatileCertificate(pkiKey, cert);
					log.debug("Installed a new certificate for SAML entity {}, DN: {}, serial: {}",
							entityId, cert.getSubjectX500Principal().getName(), 
							cert.getSerialNumber());
				}
			}
		}
	}

	protected String getCertificateKey(X509Certificate cert, String entityId, String prefix)
	{
		String dn = X500NameUtils.getComparableForm(cert.getSubjectX500Principal().getName());
		String serial = cert.getSerialNumber().toString();
		String key = prefix + DigestUtils.md5Hex(entityId) + "#" + DigestUtils.md5Hex(dn) + "#" + serial;
		return key;
	}

	protected EntityAttributesType parseMDAttributes(ExtensionsType extensions, String entityId)
	{
		if (extensions == null)
			return null;
		NodeList nl = extensions.getDomNode().getChildNodes();
		for (int i=0; i<nl.getLength(); i++)
		{
			Node elementN = nl.item(i);
			if (elementN.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element element = (Element) elementN;
			if ("EntityAttributes".equals(element.getLocalName()) && 
					"urn:oasis:names:tc:SAML:metadata:attribute".equals(element.getNamespaceURI()))
			{
				try
				{
					return EntityAttributesDocument.Factory.parse(element).getEntityAttributes();
				} catch (XmlException e)
				{
					log.warn("Can not parse entity attributes metadata extension for " + entityId, e);
				}
			}
		}
		return null;
	}
	
	protected String convertBinding(String samlBinding)
	{
		if (SAMLConstants.BINDING_HTTP_POST.equals(samlBinding))
			return Binding.HTTP_POST.toString();
		if (SAMLConstants.BINDING_HTTP_REDIRECT.equals(samlBinding))
			return Binding.HTTP_REDIRECT.toString();
		if (SAMLConstants.BINDING_SOAP.equals(samlBinding))
			return Binding.SOAP.toString();
		throw new IllegalStateException("Unsupported binding: " + samlBinding);
	}
	
	
	protected void setSLOProperty(Properties properties, String configKey, boolean noPerEntryConfig,
			EndpointType sloEndpoint, String SLOProperty, String SLORetProperty)
	{
		if (noPerEntryConfig || !properties.containsKey(configKey + SLOProperty))
		{
			if (sloEndpoint != null)
			{
				properties.setProperty(configKey + SLOProperty, 
					sloEndpoint.getLocation());
				if (SLORetProperty != null && sloEndpoint.getResponseLocation() != null)
					properties.setProperty(configKey + SLORetProperty, 
						sloEndpoint.getResponseLocation());
			}
		}
	}
}
