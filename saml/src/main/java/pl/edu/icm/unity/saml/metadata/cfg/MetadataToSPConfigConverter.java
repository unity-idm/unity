/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import static java.util.Collections.emptyList;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration.Builder;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.types.I18nString;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.ExtensionsType;
import xmlbeans.org.oasis.saml2.metadata.IDPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyTypes;
import xmlbeans.org.oasis.saml2.metadata.LocalizedNameType;
import xmlbeans.org.oasis.saml2.metadata.OrganizationType;
import xmlbeans.org.oasis.saml2.metadata.SSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesDocument;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;
import xmlbeans.org.w3.x2000.x09.xmldsig.X509DataType;

@Component
class MetadataToSPConfigConverter
{
	private static final String REFEDS_HIDE_FROM_DISCOVERY = "http://refeds.org/category/hide-from-discovery";
	private static final String MACEDIR_ENTITY_CATEGORY = "http://macedir.org/entity-category";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetadataToSPConfigConverter.class);
	private static final String SP_META_CERT = "_SP_METADATA_CERT_";
	
	private final PKIManagement pkiManagement;
	private final String defaultLocaleCode;
	
	@Autowired
	MetadataToSPConfigConverter(@Qualifier("insecure") PKIManagement pkiManagement, MessageSource msg)
	{
		this(pkiManagement, msg.getDefaultLocaleCode());
	}
	
	MetadataToSPConfigConverter(PKIManagement pkiManagement, String defaultLocaleCode)
	{
		this.pkiManagement = pkiManagement;
		this.defaultLocaleCode = defaultLocaleCode;
	}
	
	TrustedIdPs convertToTrustedIdPs(EntitiesDescriptorDocument federationMetaDoc, 
			RemoteMetadataSource metadataSource)
	{
		EntitiesDescriptorType federationMeta = federationMetaDoc.getEntitiesDescriptor();
		return new TrustedIdPs(convertToTrustedIdPs(federationMeta, metadataSource));
	}
	
	
	private Collection<TrustedIdPConfiguration> convertToTrustedIdPs(EntitiesDescriptorType federationMeta, 
			RemoteMetadataSource metadataSource)
	{
		Collection<TrustedIdPConfiguration> trustedIdPs = new ArrayList<>();
		
		EntitiesDescriptorType[] nested = federationMeta.getEntitiesDescriptorArray();
		if (nested != null)
		{
			for (EntitiesDescriptorType nestedD: nested)
				trustedIdPs.addAll(convertToTrustedIdPs(nestedD, metadataSource));
		}
		EntityDescriptorType[] entities = federationMeta.getEntityDescriptorArray();
		if (entities != null)
		{
			for (EntityDescriptorType entity: entities)
				trustedIdPs.addAll(parseEntity(federationMeta, entity, metadataSource));
		}
		return trustedIdPs;
	}
	
	private Collection<TrustedIdPConfiguration> parseEntity(EntitiesDescriptorType federationMeta, 
			EntityDescriptorType entityMeta, RemoteMetadataSource metadataSource)
	{
		IDPSSODescriptorType[] idpDefs = entityMeta.getIDPSSODescriptorArray();
		if (idpDefs == null || idpDefs.length == 0)
			return Collections.emptyList();

		Collection<TrustedIdPConfiguration> trustedIdPs = new ArrayList<>(idpDefs.length);
		for (IDPSSODescriptorType idpDef: idpDefs)
			trustedIdPs.addAll(parseEntityIdPSSO(federationMeta, entityMeta, idpDef, metadataSource));
		return trustedIdPs;
	}
	
	private List<TrustedIdPConfiguration> parseEntityIdPSSO(EntitiesDescriptorType federationMeta, 
			EntityDescriptorType entityMeta, IDPSSODescriptorType idpDef, RemoteMetadataSource metadataSource)
	{
		String entityId = entityMeta.getEntityID();
		if (metadataSource.excludedIdps.contains(entityId))
		{
			log.trace("IDP of entity {} is excluded, ignoring.", entityId);
			return emptyList();
		}
		
		EntityAttributesType entityAttributes = null;
		if (metadataSource.federationIdpsFilter != null)
		{
			entityAttributes = parseMDAttributes(entityMeta.getExtensions(), entityId);
			if (!evaluateFilterCondition(metadataSource.federationIdpsFilter,
					metadataSource.compiledFederationIdpsFilter,
					createMvelContextForFilter(entityAttributes, entityId), entityId))
			{
				log.trace("IDP of entity {} is excluded by filter, ignoring.", entityId);
				return emptyList();
			}
		}
		
		
		if (!MetaToConfigConverterHelper.supportsSaml2(idpDef))
		{
			log.trace("IDP of entity {} doesn't support SAML2 - ignoring.", entityId);
			return emptyList();
		}
		
		if (entityAttributes == null)
		{
			entityAttributes = parseMDAttributes(entityMeta.getExtensions(), entityId);
		}
		
		if (isDisabledWithREFEDSExtension(entityAttributes))
		{
			log.trace("IDP of entity {} is hidden from discovery - ignoring.", entityId);
			return emptyList();
		}
		
		KeyDescriptorType[] keys = idpDef.getKeyDescriptorArray();
		List<X509Certificate> certs = getSigningCerts(keys, entityId);
		if (certs.isEmpty())
		{
			log.info("No signing certificate found for IdP, skipping it: {}", entityId);
			return emptyList();
		}
		EndpointType webEndpoint = selectWebEndpoint(idpDef.getSingleSignOnServiceArray());
		EndpointType soapEndpoint = selectEndpointByBinding(idpDef.getSingleSignOnServiceArray(), 
				SAMLConstants.BINDING_SOAP);
		if (webEndpoint == null && soapEndpoint == null)
			return emptyList();
		
		Set<String> pkiCertNames;
		try
		{
			pkiCertNames = updatePKICerts(certs, entityId, SP_META_CERT);
		} catch (EngineException e)
		{
			log.error("Adding remote IDPs certs to local certs store failed, skipping IdP: " + entityId, e);
			return emptyList();
		}
		
		List<TrustedIdPConfiguration> ret = new ArrayList<>(2);
		if (webEndpoint != null)
		{
			Builder builder = TrustedIdPConfiguration.builder();
			fillMetadataWideSettings(builder, metadataSource, pkiCertNames, certs);
			fillIdPSettings(builder, federationMeta, entityMeta, idpDef, 1);
			fillEndpointData(builder, webEndpoint);
			ret.add(builder.build());
		}
		
		if (soapEndpoint != null)
		{
			Builder builder = TrustedIdPConfiguration.builder();
			fillMetadataWideSettings(builder, metadataSource, pkiCertNames, certs);
			fillIdPSettings(builder, federationMeta, entityMeta, idpDef, 2);
			fillEndpointData(builder, soapEndpoint);
			ret.add(builder.build());
		}
		return ret;
	}

	private Map<String, Object> createMvelContextForFilter(EntityAttributesType entityAttributes, String entityId)
	{
		Map<String, Object> context = new HashMap<>();
		context.put(FederationIdPsFilterContextKey.entityID.name(), entityId);
		Map<String, List<String>> attributes = new HashMap<>();
		if (entityAttributes != null)
		{
			AttributeType[] attributeArray = entityAttributes.getAttributeArray();
			for (AttributeType a : attributeArray)
			{
				attributes.put(a.getName(), getAttributeValues(a));
			}
		}
		context.put(FederationIdPsFilterContextKey.attributes.name(), attributes);
		log.trace("Created MVEL context for entity {}: {}", entityId, context);

		return context;
	}
	
	private List<String> getAttributeValues(AttributeType a)
	{	
		List<String> ret = new ArrayList<>();
		for (XmlObject value : a.getAttributeValueArray())
		{
			XmlCursor c = value.newCursor();
			String valueStr = c.getTextValue();
			c.dispose();
			ret.add(valueStr);
		}
		
		return ret;	
	}
	
	private boolean evaluateFilterCondition(String condition, Serializable compiledCondition, Object input, String entityId) 
	{
		if (condition == null)
		{
			return true;
		}
		
		Boolean result = null;
		try
		{
			result = (Boolean) MVEL.executeExpression(compiledCondition, input, new HashMap<>());
		} catch (Exception e)
		{
			log.warn("Error during expression execution.", e);
		}

		if (result == null)
		{
			log.trace("Condition evaluated for IDP of entity {} is evaluated to null value, assuming false.", entityId);
			return false;
		}
		log.trace("Condition \"{}\" evaluated for IDP of entity {} is evaluated to {}", condition, entityId, result.booleanValue());
		return result.booleanValue();
	}

	

	private void fillMetadataWideSettings(Builder builder, RemoteMetadataSource metadataSource, 
			Set<String> pkiCertNames, List<X509Certificate> certs)
	{
		builder.withRegistrationForm(metadataSource.registrationForm);
		builder.withCertificateNames(pkiCertNames);
		builder.withTranslationProfile(metadataSource.translationProfile);
		builder.withPublicKeys(certs.stream().map(X509Certificate::getPublicKey).collect(Collectors.toList()));
	}

	private void fillIdPSettings(Builder builder, EntitiesDescriptorType federationMeta, 
			EntityDescriptorType entityMeta, IDPSSODescriptorType idpDef, int index)
	{
		String federationId = federationMeta.getID();
		String federationName = federationMeta.getName();
		
		
		String entityId = entityMeta.getEntityID();
		UIInfoType uiInfo = MetaToConfigConverterHelper.parseMDUIInfo(idpDef.getExtensions(), entityId);
		
		builder.withKey(TrustedIdPKey.metadataEntity(entityId, index))
			.withSamlId(entityId)
			.withFederationId(federationId)
			.withFederationName(federationName)
			.withSignRequest(idpDef.isSetWantAuthnRequestsSigned())
			.withName(getLocalizedNamesAsI18nString(uiInfo, idpDef, entityMeta))
			.withLogoURI(getLocalizedLogosAsI18nString(uiInfo));
		
		EndpointType redirectSLOEndpoint = selectEndpointByBinding(idpDef.getSingleLogoutServiceArray(),
				SAMLConstants.BINDING_HTTP_REDIRECT);
		EndpointType postSLOEndpoint = selectEndpointByBinding(idpDef.getSingleLogoutServiceArray(),
				SAMLConstants.BINDING_HTTP_POST);
		EndpointType soapSLOEndpoint = selectEndpointByBinding(idpDef.getSingleLogoutServiceArray(), 
				SAMLConstants.BINDING_SOAP);
		addSLOEndpoint(builder, soapSLOEndpoint, Binding.SOAP);
		addSLOEndpoint(builder, postSLOEndpoint, Binding.HTTP_POST);
		addSLOEndpoint(builder, redirectSLOEndpoint, Binding.HTTP_REDIRECT);
	}

	private void addSLOEndpoint(Builder builder, EndpointType soapSLOEndpoint, Binding binding)
	{
		if (soapSLOEndpoint != null)
			builder.withLogoutEndpoint(new SAMLEndpointDefinition(binding, 
				soapSLOEndpoint.getLocation(), soapSLOEndpoint.getResponseLocation()));
	}
	
	private void fillEndpointData(Builder builder, 
			EndpointType endpoint)
	{
		builder
			.withBinding(Binding.ofSAMLBinding(endpoint.getBinding()))
			.withIdpEndpointURL(endpoint.getLocation());
	}
	
	private static boolean isDisabledWithREFEDSExtension(EntityAttributesType entityAttributes)
	{
		if (entityAttributes == null)
			return false;
		AttributeType[] attributeArray = entityAttributes.getAttributeArray();
		for (AttributeType a: attributeArray)
		{
			if (MACEDIR_ENTITY_CATEGORY.equals(a.getName()))
			{
				for (XmlObject value : a.getAttributeValueArray())
				{
					XmlCursor c = value.newCursor();
					String valueStr = c.getTextValue();
					c.dispose();
					if (valueStr.equals(REFEDS_HIDE_FROM_DISCOVERY))
						return true;
				}
			}
		}
		return false;
	}
	
	private static EntityAttributesType parseMDAttributes(ExtensionsType extensions, String entityId)
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
	
	private static List<X509Certificate> getSigningCerts(KeyDescriptorType[] keys, String entityId)
	{
		List<X509Certificate> ret = new ArrayList<>();
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
						CertificateFactory instance = CertificateFactory.getInstance("X.509");
						cert = (X509Certificate) instance.generateCertificate(new ByteArrayInputStream(certsAsBytes[0]));
					} catch (CertificateException e)
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
	
	
	private Set<String> updatePKICerts(List<X509Certificate> certs, String entityId, String prefix)
			throws EngineException
	{
		Set<String> keysOfCerts = new HashSet<>(); 
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
				keysOfCerts.add(pkiKey);
			}
		}
		return keysOfCerts;
	}

	private static String getCertificateKey(X509Certificate cert, String entityId, String prefix)
	{
		String dn = X500NameUtils.getComparableForm(cert.getSubjectX500Principal().getName());
		String serial = cert.getSerialNumber().toString();
		String key = prefix + DigestUtils.md5Hex(entityId) + "#" + DigestUtils.md5Hex(dn) + "#" + serial;
		return key;
	}
	
	
	private static EndpointType selectWebEndpoint(EndpointType[] endpoints)
	{
		EndpointType selectedEndpoint = null;
		for (EndpointType endpoint: endpoints)
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
	
	private static EndpointType selectEndpointByBinding(EndpointType[] endpoints, String requestedBinding)
	{
		for (EndpointType endpoint: endpoints)
		{
			if (endpoint.getBinding() == null || endpoint.getLocation() == null)
				continue;
			if (endpoint.getBinding().equals(requestedBinding))
				return endpoint;
		}
		return null;
	}
	
	private I18nString getLocalizedNamesAsI18nString(UIInfoType uiInfo,
			SSODescriptorType idpDesc, EntityDescriptorType mainDescriptor)
	{
		I18nString ret = new I18nString();
		ret.addAllValues(getLocalizedNames(uiInfo, idpDesc, mainDescriptor));
		return ret;
	}
	
	private Map<String, String> getLocalizedNames(UIInfoType uiInfo,
			SSODescriptorType idpDesc, EntityDescriptorType mainDescriptor)
	{
		Map<String, String> ret = new HashMap<>();
		OrganizationType mainOrg = mainDescriptor.getOrganization();
		if (mainOrg != null)
		{
			addLocalizedNames(mainOrg.getOrganizationNameArray(), ret);
			addLocalizedNames(mainOrg.getOrganizationDisplayNameArray(), ret);
		}
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

	private static I18nString getLocalizedLogosAsI18nString(UIInfoType uiInfo)
	{
		I18nString ret = new I18nString();
		Map<String, LogoType> asMap = getLocalizedLogos(uiInfo);
		ret.addAllValues(asMap.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey(), 
						entry -> entry.getValue().getStringValue())));
		if (asMap.containsKey(""))
			ret.setDefaultValue(asMap.get("").getStringValue());
		return ret;
	}
	
	private static Map<String, LogoType> getLocalizedLogos(UIInfoType uiInfo)
	{
		Map<String, LogoType> ret = new HashMap<>();
		if (uiInfo != null)
		{
			LogoType[] logos = uiInfo.getLogoArray();
			if (logos == null)
				return ret;
			for (LogoType logo : logos)
			{
				String key = logo.getLang() == null ? "" : logo.getLang();
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
	
	private void addLocalizedNames(LocalizedNameType[] names, Map<String, String> ret)
	{
		if (names == null)
			return;
		String enName = null;
		for (LocalizedNameType name : names)
		{
			String lang = name.getLang();
			if (lang != null)
			{
				ret.put(lang, name.getStringValue());
				if (lang.equals(defaultLocaleCode))
					ret.put("", name.getStringValue());
				if (lang.equals("en"))
					enName = name.getStringValue();
			} else
			{
				ret.put("", name.getStringValue());
			}
		}
		if (enName != null && !ret.containsKey(""))
			ret.put("", enName);
	}
}
