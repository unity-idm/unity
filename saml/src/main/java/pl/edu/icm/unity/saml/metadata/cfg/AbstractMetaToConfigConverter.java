/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.SAMLProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.ExtensionsType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyTypes;
import xmlbeans.org.oasis.saml2.metadata.LocalizedNameType;
import xmlbeans.org.oasis.saml2.metadata.OrganizationType;
import xmlbeans.org.oasis.saml2.metadata.SSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoDocument;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;
import xmlbeans.org.w3.x2000.x09.xmldsig.X509DataType;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.samly2.SAMLConstants;

/**
 * Base for converters of SAML metadata into a series of property statements.
 *  
 * @author K. Benedyczak
 */
public abstract class AbstractMetaToConfigConverter
{
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AbstractMetaToConfigConverter.class);
	protected PKIManagement pkiManagement;
	
	/**
	 * Inserts metadata to the configuration in properties. All entries which are present in 
	 * realConfig are preserved. 
	 * @param meta
	 * @param properties
	 * @param realConfig
	 */
	protected void convertToProperties(EntitiesDescriptorDocument metaDoc, Properties properties, 
			SAMLProperties realConfig, String configKey)
	{
		EntitiesDescriptorType meta = metaDoc.getEntitiesDescriptor();
		convertToProperties(meta, properties, realConfig, configKey);
	}
	
	protected void convertToProperties(EntitiesDescriptorType meta, Properties properties, 
			SAMLProperties realConfig, String configKey)
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
				convertToProperties(entity, properties, realConfig, configKey);
			}
		}
	}
	
	protected abstract void convertToProperties(EntityDescriptorType meta, Properties properties, 
			SAMLProperties realConfig, String configKey);
	
	
	protected boolean supportsSaml2(SSODescriptorType idpDef)
	{
		List<?> supportedProtocols = idpDef.getProtocolSupportEnumeration();
		for (Object supported: supportedProtocols)
			if (SAMLConstants.PROTOCOL_NS.equals(supported))
				return true;
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
	
	
	protected void updatePKICerts(List<X509Certificate> certs, String entityId, String prefix) throws EngineException
	{
		synchronized (pkiManagement)
		{
			
		
		for (X509Certificate cert: certs)
		{
			String pkiKey = getCertificateKey(cert, entityId, prefix);
			try
			{
				X509Certificate existingCert = pkiManagement.getCertificate(pkiKey);
				if (!existingCert.equals(cert))
				{
					pkiManagement.updateCertificate(pkiKey, cert);
				}
			} catch (WrongArgumentException e)
			{
				pkiManagement.addCertificate(pkiKey, cert);
			}
		}}
	}
	
	protected String getCertificateKey(X509Certificate cert, String entityId, String prefix)
	{
		String dn = X500NameUtils.getComparableForm(cert.getSubjectX500Principal().getName());
		String key = prefix + DigestUtils.md5Hex(entityId) + "#" + DigestUtils.md5Hex(dn);
		return key;
	}
	
	
	protected Map<String, String> getLocalizedNames(UIInfoType uiInfo, SSODescriptorType idpDesc)
	{
		Map<String, String> ret = new HashMap<String, String>();
		OrganizationType org = idpDesc.getOrganization();
		if (org != null)
		{
			addLocalizedNames(org.getOrganizationNameArray(), ret);
			addLocalizedNames(org.getOrganizationDisplayNameArray(), ret);
		}
		if (uiInfo != null)
		{
			addLocalizedNames(uiInfo.getDisplayNameArray(), ret);
		}
		return ret;
	}

	protected Map<String, LogoType> getLocalizedLogos(UIInfoType uiInfo)
	{
		Map<String, LogoType> ret = new HashMap<String, LogoType>();
		if (uiInfo != null)
		{
			LogoType[] logos = uiInfo.getLogoArray();
			if (logos == null)
				return ret;
			for (LogoType logo: logos)
			{
				String key = logo.getLang() == null ? "" : "." + logo.getLang();
				LogoType e = ret.get(key);
				if (e == null)
				{
					ret.put(key, logo);
				} else
				{
					if (e.getHeight().longValue() < logo.getHeight().longValue())
						ret.put(key, logo);
				}
			}
		}
		
		return ret;
	}
	
	protected void addLocalizedNames(LocalizedNameType[] names, Map<String, String> ret)
	{
		if (names == null)
			return;
		for (LocalizedNameType name: names)
		{
			String lang = name.getLang();
			if (lang != null)
				ret.put("." + lang, name.getStringValue());
		}
	}
	
	protected UIInfoType parseMDUIInfo(ExtensionsType extensions, String entityId)
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
			if ("UIInfo".equals(element.getLocalName()) && 
					"urn:oasis:names:tc:SAML:metadata:ui".equals(element.getNamespaceURI()))
			{
				try
				{
					return UIInfoDocument.Factory.parse(element).getUIInfo();
				} catch (XmlException e)
				{
					log.warn("Can not parse UIInfo metadata extension for " + entityId, e);
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
}
