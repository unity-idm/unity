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
import pl.edu.icm.unity.saml.SAMLProperties;
import pl.edu.icm.unity.saml.idp.SAMLIDPProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.SPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;
import eu.unicore.samly2.SAMLConstants;

/**
 * Utility class: converts SAML metadata into a series of property statements, 
 * compatible with {@link SAMLIDPProperties}. If the SAML metadata is describing an SP which is already defined in 
 * the source properties, then only the entries which are not already set are added.
 *  
 * @author P. Piernik
 */

public class MetaToIDPConfigConverter extends AbstractMetaToConfigConverter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetaToSPConfigConverter.class);
	
	public MetaToIDPConfigConverter(PKIManagement pkiManagement)
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
			SAMLIDPProperties realConfig, String configKey)
	{
		super.convertToProperties(metaDoc, properties, realConfig, configKey);
	}
	
	
	@Override
	protected void convertToProperties(EntityDescriptorType meta, Properties properties,
			SAMLProperties realConfigG, String configKey)
	{
		SAMLIDPProperties realConfig = (SAMLIDPProperties) realConfigG;
		SPSSODescriptorType[] spDefs = meta.getSPSSODescriptorArray();
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
			
			KeyDescriptorType[] keys = spDef.getKeyDescriptorArray();
			List<X509Certificate> certs = getSigningCerts(keys, entityId);
			if (certs.isEmpty())
			{
				log.info("No signing certificate found for sp, skipping it: " + 
						entityId);
				continue;
			}
			
			IndexedEndpointType aserServ = selectHttpPostService(spDef);
			if (aserServ == null)
				continue;
			
			try
			{
				updatePKICerts(certs, entityId, "_IDP_METADATA_CERT_");
			} catch (EngineException e)
			{
				log.error("Adding remote Sps certs to local certs store failed, "
						+ "skipping IdP: " + entityId, e);
				continue;
			}
		
			UIInfoType uiInfo = parseMDUIInfo(spDef.getExtensions(), entityId);
			Map<String, String> names = getLocalizedNames(uiInfo, spDef);
			Map<String, LogoType> logos = getLocalizedLogos(uiInfo);
			
			//TODO SIGN
			addEntryToProperties(entityId, aserServ, false, realConfig, configKey, properties, r, certs, names, logos);
		
		
			
		}
		
	}
	
	//TODO add next properties 
	//TODO requireSingned
	private void addEntryToProperties(String entityId, IndexedEndpointType serviceEndpoint, boolean requireSignedReq,
			SAMLIDPProperties realConfig, String metaConfigKey, Properties properties, Random r, 
			List<X509Certificate> certs,
			Map<String, String> names, Map<String, LogoType> logos)
	{
		String configKey = getExistingKey(entityId, realConfig);
		
		boolean noPerSpConfig = configKey == null;
		if (configKey == null)
			configKey = SAMLIDPProperties.P + SAMLIDPProperties.ALLOWED_SP_PREFIX + 
					"_entryFromMetadata_" + r.nextInt() + "."; 

		if (noPerSpConfig || !properties.containsKey(configKey + SAMLIDPProperties.ALLOWED_SP_ENTITY))
			properties.setProperty(configKey + SAMLIDPProperties.ALLOWED_SP_ENTITY, 
					entityId);
		if (noPerSpConfig || !properties.containsKey(configKey + SAMLIDPProperties.ALLOWED_SP_RETURN_URL))
			properties.setProperty(configKey + SAMLIDPProperties.ALLOWED_SP_RETURN_URL, 
					serviceEndpoint.getLocation());
		
		if (noPerSpConfig || !properties.containsKey(configKey + SAMLIDPProperties.ALLOWED_SP_CERTIFICATE))
			properties.setProperty(configKey + SAMLIDPProperties.ALLOWED_SP_CERTIFICATE, 
					serviceEndpoint.getLocation());
		
		
		
		
		
		
		
		
		
		
		log.debug("Added a accepted SP loaded from SAML metadata: " + entityId + " with " + 
				serviceEndpoint.getLocation() + " returnl url" + logos);
		
	}
	
	
	private String getExistingKey(String entityId, SAMLIDPProperties realConfig)
	{
		Set<String> keys = realConfig.getStructuredListKeys(SAMLIDPProperties.ALLOWED_SP_PREFIX);
		for (String key: keys)
		{
			if (entityId.equals(realConfig.getValue(key+SAMLIDPProperties.ALLOWED_SP_ENTITY)))
				return SAMLIDPProperties.P + key;
		}
		return null;
	}

	private IndexedEndpointType selectHttpPostService(SPSSODescriptorType spDef)
	{
	
		for (IndexedEndpointType endpoint: spDef.getAssertionConsumerServiceArray())
		{
			if (endpoint.getBinding() == null || endpoint.getLocation() == null)
				continue;
			
			if(endpoint.getBinding().equals(SAMLConstants.BINDING_HTTP_POST))
				return endpoint;
		}
	
		//TODO
		return null;
	}



}
