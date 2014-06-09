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
import java.util.Random;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.ExtensionsType;
import xmlbeans.org.oasis.saml2.metadata.IDPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyTypes;
import xmlbeans.org.oasis.saml2.metadata.LocalizedNameType;
import xmlbeans.org.oasis.saml2.metadata.OrganizationType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoDocument;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;
import xmlbeans.org.w3.x2000.x09.xmldsig.X509DataType;

/**
 * Utility class: converts SAML metadata into a series of property statements, 
 * compatible with {@link SAMLSPProperties}. If the SAML metadata is describing an IdP which is already defined in 
 * the source properties, then only the entries which are not already set are added.
 *  
 * @author K. Benedyczak
 */
public class MetaToSPConfigConverter extends AbstractMetaToConfigConverter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetaToSPConfigConverter.class);
	private PKIManagement pkiManagement;
	
	public MetaToSPConfigConverter(PKIManagement pkiManagement)
	{
		this.pkiManagement = pkiManagement;
	}

	/**
	 * Inserts metadata to the configuration in properties. All entries which are present in 
	 * realConfig are preserved. 
	 * @param meta
	 * @param properties
	 * @param realConfig
	 */
	public void convertToProperties(EntitiesDescriptorDocument metaDoc, Properties properties, 
			SAMLSPProperties realConfig, String configKey)
	{
		super.convertToProperties(metaDoc, properties, realConfig, configKey);
	}
	
	protected void convertToProperties(EntityDescriptorType meta, Properties properties, 
			SamlProperties realConfigG, String configKey)
	{
		SAMLSPProperties realConfig = (SAMLSPProperties) realConfigG;
		IDPSSODescriptorType[] idpDefs = meta.getIDPSSODescriptorArray();
		if (idpDefs == null || idpDefs.length == 0)
			return;

		String entityId = meta.getEntityID();
		Random r = new Random(); 
		for (IDPSSODescriptorType idpDef: idpDefs)
		{
			if (!supportsSaml2(idpDef))
			{
				log.trace("IDP of entity " + entityId +	" doesn't support SAML2 - ignoring.");
				continue;
			}
			
			KeyDescriptorType[] keys = idpDef.getKeyDescriptorArray();
			List<X509Certificate> certs = getSigningCerts(keys, entityId);
			if (certs.isEmpty())
			{
				log.info("No signing certificate found for IdP, skipping it: " + 
						entityId);
				continue;
			}
			boolean requireSignedReq = idpDef.isSetWantAuthnRequestsSigned();
			EndpointType webEndpoint = selectWebEndpoint(idpDef);
			EndpointType soapEndpoint = selectSOAPEndpoint(idpDef);
			if (webEndpoint == null && soapEndpoint == null)
				continue;
			
			try
			{
				updatePKICerts(certs, entityId);
			} catch (EngineException e)
			{
				log.error("Adding remote IDPs certs to local certs store failed, "
						+ "skipping IdP: " + entityId, e);
				continue;
			}
			UIInfoType uiInfo = parseMDUIInfo(idpDef.getExtensions(), entityId);
			Map<String, String> names = getLocalizedNames(uiInfo, idpDef);
			Map<String, LogoType> logos = getLocalizedLogos(uiInfo);
			
			if (webEndpoint != null)
			{
				addEntryToProperties(entityId, webEndpoint, requireSignedReq, realConfig, 
						configKey, properties, r, certs, names, logos);
			}
			
			if (soapEndpoint != null)
			{
				addEntryToProperties(entityId, soapEndpoint, requireSignedReq, realConfig, 
						configKey, properties, r, certs, names, logos);
			}
		}
	}
	
	private void addEntryToProperties(String entityId, EndpointType endpoint, boolean requireSignedReq,
			SAMLSPProperties realConfig, String metaConfigKey, Properties properties, Random r, 
			List<X509Certificate> certs,
			Map<String, String> names, Map<String, LogoType> logos)
	{
		String configKey = getExistingKey(entityId, endpoint, realConfig);
		String perMetaProfile = realConfig.getValue(metaConfigKey + 
				SAMLSPProperties.IDPMETA_TRANSLATION_PROFILE);
		String perMetaRegForm = realConfig.getValue(metaConfigKey + 
				SAMLSPProperties.IDPMETA_REGISTRATION_FORM);
	
		boolean noPerIdpConfig = configKey == null;
		if (configKey == null)
			configKey = SAMLSPProperties.P + SAMLSPProperties.IDP_PREFIX + 
					"_entryFromMetadata_" + r.nextInt() + "."; 

		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_ID))
			properties.setProperty(configKey + SAMLSPProperties.IDP_ID, 
					entityId);
		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_BINDING))
			properties.setProperty(configKey + SAMLSPProperties.IDP_BINDING, 
					convertBinding(endpoint.getBinding()));
		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_ADDRESS))
			properties.setProperty(configKey + SAMLSPProperties.IDP_ADDRESS, 
					endpoint.getLocation());
		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_CERTIFICATE))
		{
			int i = 1;
			for (X509Certificate cert: certs)
			{
				if (!properties.containsKey(configKey + SAMLSPProperties.IDP_CERTIFICATES + i))
					properties.setProperty(configKey + SAMLSPProperties.IDP_CERTIFICATES + i, 
							getCertificateKey(cert, entityId));
				i++;
			}
		}
		for (Map.Entry<String, String> name: names.entrySet())
		{
			if (noPerIdpConfig || !properties.containsKey(configKey + 
					SAMLSPProperties.IDP_NAME + name.getKey()))
				properties.setProperty(configKey + SAMLSPProperties.IDP_NAME + name.getKey(), 
						name.getValue());
		}
		for (Map.Entry<String, LogoType> logo: logos.entrySet())
		{
			if (noPerIdpConfig || !properties.containsKey(configKey + 
					SAMLSPProperties.IDP_LOGO + logo.getKey()))
				properties.setProperty(configKey + SAMLSPProperties.IDP_LOGO + logo.getKey(), 
						logo.getValue().getStringValue());
		}

		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_SIGN_REQUEST))
			properties.setProperty(configKey + SAMLSPProperties.IDP_SIGN_REQUEST, 
					Boolean.toString(requireSignedReq));
		if ((perMetaProfile != null) && (noPerIdpConfig || 
				!properties.containsKey(configKey + SAMLSPProperties.IDP_TRANSLATION_PROFILE)))
			properties.setProperty(configKey + SAMLSPProperties.IDP_TRANSLATION_PROFILE, 
					perMetaProfile);
		if (perMetaRegForm != null && (noPerIdpConfig || 
				!properties.containsKey(configKey + SAMLSPProperties.IDP_REGISTRATION_FORM)))
			properties.setProperty(configKey + SAMLSPProperties.IDP_REGISTRATION_FORM, 
					perMetaRegForm);
		
		log.debug("Added a trusted IdP loaded from SAML metadata: " + entityId);
	}

	private void updatePKICerts(List<X509Certificate> certs, String entityId) throws EngineException
	{
		for (X509Certificate cert: certs)
		{
			String pkiKey = getCertificateKey(cert, entityId);
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
		}
	}
	
	private String getCertificateKey(X509Certificate cert, String entityId)
	{
		String dn = X500NameUtils.getComparableForm(cert.getSubjectX500Principal().getName());
		String key = "_SP_METADATA_CERT_" + DigestUtils.md5Hex(entityId) + "#" + DigestUtils.md5Hex(dn);
		return key;
	}
	
	private List<X509Certificate> getSigningCerts(KeyDescriptorType[] keys, String entityId)
	{
		List<X509Certificate> ret = new ArrayList<X509Certificate>();
		for (KeyDescriptorType key: keys)
		{
			if (KeyTypes.SIGNING.equals(key.getUse()))
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
	
	private String convertBinding(String samlBinding)
	{
		if (SAMLConstants.BINDING_HTTP_POST.equals(samlBinding))
			return Binding.HTTP_POST.toString();
		if (SAMLConstants.BINDING_HTTP_REDIRECT.equals(samlBinding))
			return Binding.HTTP_REDIRECT.toString();
		if (SAMLConstants.BINDING_SOAP.equals(samlBinding))
			return Binding.SOAP.toString();
		throw new IllegalStateException("Unsupported binding: " + samlBinding);
	}
	
	private String getExistingKey(String entityId, EndpointType endpoint, SAMLSPProperties realConfig)
	{
		Set<String> keys = realConfig.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		for (String key: keys)
		{
			if (entityId.equals(realConfig.getValue(key+SAMLSPProperties.IDP_ID)))
				return SAMLSPProperties.P + key;
		}
		return null;
	}
	
	private boolean supportsSaml2(IDPSSODescriptorType idpDef)
	{
		List<?> supportedProtocols = idpDef.getProtocolSupportEnumeration();
		for (Object supported: supportedProtocols)
			if (SAMLConstants.PROTOCOL_NS.equals(supported))
				return true;
		return false;
	}
	
	private EndpointType selectWebEndpoint(IDPSSODescriptorType idpDef)
	{
		EndpointType selectedEndpoint = null;
		for (EndpointType endpoint: idpDef.getSingleSignOnServiceArray())
		{
			if (endpoint.getBinding() == null || endpoint.getLocation() == null)
				continue;
			if (endpoint.getBinding().equals(SAMLConstants.BINDING_HTTP_REDIRECT))
				return endpoint;
			if (endpoint.getBinding().equals(SAMLConstants.BINDING_HTTP_POST))
				selectedEndpoint = endpoint;
		}
		return selectedEndpoint;
	}

	private EndpointType selectSOAPEndpoint(IDPSSODescriptorType idpDef)
	{
		for (EndpointType endpoint: idpDef.getSingleSignOnServiceArray())
		{
			if (endpoint.getBinding() == null || endpoint.getLocation() == null)
				continue;
			if (endpoint.getBinding().equals(SAMLConstants.BINDING_SOAP))
				return endpoint;
		}
		return null;
	}
	
	
	private Map<String, String> getLocalizedNames(UIInfoType uiInfo, IDPSSODescriptorType idpDesc)
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

	private Map<String, LogoType> getLocalizedLogos(UIInfoType uiInfo)
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
					if (e.getHeight().longValue() < logo.getHeight().longValue() && 
							e.getWidth().longValue() < logo.getWidth().longValue())
						ret.put(key, logo);
				}
			}
		}
		
		return ret;
	}
	
	private void addLocalizedNames(LocalizedNameType[] names, Map<String, String> ret)
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
	
	private UIInfoType parseMDUIInfo(ExtensionsType extensions, String entityId)
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
}


