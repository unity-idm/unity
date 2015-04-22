/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties.RequestAcceptancePolicy;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.SPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;
import eu.unicore.samly2.SAMLConstants;

/**
 * Utility class: converts SAML metadata into a series of property statements, 
 * compatible with {@link SamlIdpProperties}. If the SAML metadata is describing an SP which is already defined in 
 * the source properties, then only the entries which are not already set are added.
 *  
 * @author P. Piernik
 */

public class MetaToIDPConfigConverter extends AbstractMetaToConfigConverter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetaToIDPConfigConverter.class);
	private static final String IDP_META_CERT = "_IDP_METADATA_CERT_";
	
	public MetaToIDPConfigConverter(PKIManagement pkiManagement, UnityMessageSource msg)
	{
		super(pkiManagement, msg);
	}
	
	/**
	 * Inserts metadata to the configuration in properties. All entries which are present in 
	 * realConfig are preserved. 
	 * @param meta
	 * @param properties
	 * @param realConfig
	 */
	public void convertToProperties(EntitiesDescriptorDocument metaDoc, Properties properties, 
			SamlIdpProperties realConfig, String configKey)
	{
		super.convertToProperties(metaDoc, properties, realConfig, configKey);
	}
	
	
	@Override
	protected void convertToProperties(EntityDescriptorType meta, Properties properties,
			SamlProperties realConfigG, String configKey)
	{
		SamlIdpProperties realConfig = (SamlIdpProperties) realConfigG;
		SPSSODescriptorType[] spDefs = meta.getSPSSODescriptorArray();
		RequestAcceptancePolicy trustMode = realConfig.getEnumValue(SamlIdpProperties.SP_ACCEPT_POLICY, 
				RequestAcceptancePolicy.class);
		if (spDefs == null || spDefs.length == 0)
			return;
	
		String entityId = meta.getEntityID();
		Random r = new Random(); 
		for (SPSSODescriptorType spDef: spDefs)
		{
			if (!supportsSaml2(spDef))
			{
				log.trace("SP of entity " + entityId +	" doesn't support SAML2 - ignoring.");
				continue;
			}
			EntityAttributesType entityAttributes = parseMDAttributes(meta.getExtensions(), entityId);
			if (isDisabled(entityAttributes))
			{
				log.trace("SP of entity " + entityId +	" is hidden from discovery - ignoring.");
				continue;
			}
			
			KeyDescriptorType[] keys = spDef.getKeyDescriptorArray();
			List<X509Certificate> certs = getSigningCerts(keys, entityId);
			if (!certs.isEmpty())
			{
				try
				{
					updatePKICerts(certs, entityId, IDP_META_CERT );
				} catch (EngineException e)
				{
					log.error("Adding remote SPs certs to local certs store failed, "
							+ "skipping IdP: " + entityId, e);
					continue;
				}
			} else if (trustMode == RequestAcceptancePolicy.strict)
			{
				log.info("No signing certificate found for SP, skipping it as "
						+ "the 'strict' trust model is used: " + entityId);
				continue;
			}
			
			EndpointType aserServ = selectEndpointByBinding(spDef.getAssertionConsumerServiceArray(), 
					SAMLConstants.BINDING_HTTP_POST);
			if (aserServ == null)
				continue;

			EndpointType redirectSLOEndpoint = selectEndpointByBinding(spDef.getSingleLogoutServiceArray(),
					SAMLConstants.BINDING_HTTP_REDIRECT);
			EndpointType postSLOEndpoint = selectEndpointByBinding(spDef.getSingleLogoutServiceArray(),
					SAMLConstants.BINDING_HTTP_POST);
			EndpointType soapSLOEndpoint = selectEndpointByBinding(spDef.getSingleLogoutServiceArray(), 
					SAMLConstants.BINDING_SOAP);
		
			UIInfoType uiInfo = parseMDUIInfo(spDef.getExtensions(), entityId);
			Map<String, String> names = getLocalizedNames(uiInfo, spDef, meta);
			Map<String, LogoType> logos = getLocalizedLogos(uiInfo);
				
			addEntryToProperties(entityId, aserServ, soapSLOEndpoint, postSLOEndpoint, redirectSLOEndpoint,
					realConfig, configKey, properties, r, 
					certs, names, logos);					
		}		
	}
	
	private void addEntryToProperties(String entityId, EndpointType serviceEndpoint,
			EndpointType sloSoapEndpoint, EndpointType sloPostEndpoint, EndpointType sloRedirectEndpoint,
			SamlIdpProperties realConfig, String metaConfigKey, Properties properties,
			Random r, List<X509Certificate> certs, Map<String, String> names,
			Map<String, LogoType> logos)
	{
		String configKey = getExistingKey(entityId, realConfig);
		
		boolean noPerSpConfig = configKey == null;
		if (configKey == null)
			configKey = SamlIdpProperties.P + SamlIdpProperties.ALLOWED_SP_PREFIX + 
					"_entryFromMetadata_" + r.nextInt() + "."; 

		if (noPerSpConfig || !properties.containsKey(configKey + SamlIdpProperties.ALLOWED_SP_ENTITY))
			properties.setProperty(configKey + SamlIdpProperties.ALLOWED_SP_ENTITY, 
					entityId);
		if (noPerSpConfig || !properties.containsKey(configKey + SamlIdpProperties.ALLOWED_SP_RETURN_URL))
			properties.setProperty(configKey + SamlIdpProperties.ALLOWED_SP_RETURN_URL, 
					serviceEndpoint.getLocation());
		setSLOProperty(properties, configKey, noPerSpConfig, sloSoapEndpoint, 
				SamlProperties.SOAP_LOGOUT_URL, null);
		setSLOProperty(properties, configKey, noPerSpConfig, sloPostEndpoint, 
				SamlProperties.POST_LOGOUT_URL, SamlProperties.POST_LOGOUT_RET_URL);
		setSLOProperty(properties, configKey, noPerSpConfig, sloRedirectEndpoint, 
				SamlProperties.REDIRECT_LOGOUT_URL, SamlProperties.REDIRECT_LOGOUT_RET_URL);
		
		if (noPerSpConfig || !properties.containsKey(configKey + SamlIdpProperties.ALLOWED_SP_CERTIFICATE))
		{
			int i = 1;
			for (X509Certificate cert: certs)
			{
				if (!properties.containsKey(configKey + SamlIdpProperties.ALLOWED_SP_CERTIFICATES + i))
					properties.setProperty(configKey + SamlIdpProperties.ALLOWED_SP_CERTIFICATES + i, 
							getCertificateKey(cert, entityId, IDP_META_CERT));
				i++;
			}
		}
		
		for (Map.Entry<String, String> name: names.entrySet())
		{
			if (noPerSpConfig || !properties.containsKey(configKey + 
					SamlIdpProperties.ALLOWED_SP_NAME + name.getKey()))
				properties.setProperty(configKey + SamlIdpProperties.ALLOWED_SP_NAME + name.getKey(), 
						name.getValue());
		}
		
		for (Map.Entry<String, LogoType> logo: logos.entrySet())
		{
			if (noPerSpConfig || !properties.containsKey(configKey + 
					SamlIdpProperties.ALLOWED_SP_LOGO + logo.getKey()))
				properties.setProperty(configKey + SamlIdpProperties.ALLOWED_SP_LOGO + logo.getKey(), 
						logo.getValue().getStringValue());
		}
					
		log.debug("Added an accepted SP loaded from SAML metadata: " + entityId + " with " + 
				serviceEndpoint.getLocation() + " return url");		
	}
		
	private String getExistingKey(String entityId, SamlIdpProperties realConfig)
	{
		Set<String> keys = realConfig.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX);
		for (String key: keys)
		{
			if (entityId.equals(realConfig.getValue(key+SamlIdpProperties.ALLOWED_SP_ENTITY)))
				return SamlIdpProperties.P + key;
		}
		return null;
	}

	private EndpointType selectEndpointByBinding(EndpointType[] endpoints, String binding)
	{
		for (EndpointType endpoint: endpoints)
		{
			if (endpoint.getBinding() == null || endpoint.getLocation() == null)
				continue;
			
			if(endpoint.getBinding().equals(binding))
				return endpoint;
		}
		return null;
	}
}
