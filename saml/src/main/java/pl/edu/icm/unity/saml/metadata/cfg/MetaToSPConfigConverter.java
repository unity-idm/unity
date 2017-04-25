/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.IDPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.KeyDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;

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
	private static final String SP_META_CERT = "_SP_METADATA_CERT_";
	
	public MetaToSPConfigConverter(PKIManagement pkiManagement, UnityMessageSource msg)
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
			SAMLSPProperties realConfig, String configKey)
	{
		super.convertToProperties(metaDoc, properties, realConfig, configKey);
	}
	
	@Override
	protected void convertToProperties(EntityDescriptorType meta, Properties properties, 
			SamlProperties realConfigG, String configKey)
	{
		SAMLSPProperties realConfig = (SAMLSPProperties) realConfigG;
		IDPSSODescriptorType[] idpDefs = meta.getIDPSSODescriptorArray();
		if (idpDefs == null || idpDefs.length == 0)
			return;

		String entityId = meta.getEntityID();
		for (IDPSSODescriptorType idpDef: idpDefs)
		{
			if (!supportsSaml2(idpDef))
			{
				log.trace("IDP of entity " + entityId +	" doesn't support SAML2 - ignoring.");
				continue;
			}
			
			EntityAttributesType entityAttributes = parseMDAttributes(meta.getExtensions(), entityId);
			if (isDisabled(entityAttributes))
			{
				log.trace("IDP of entity " + entityId +	" is hidden from discovery - ignoring.");
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
			EndpointType webEndpoint = selectWebEndpoint(idpDef.getSingleSignOnServiceArray());
			EndpointType soapEndpoint = selectEndpointByBinding(idpDef.getSingleSignOnServiceArray(), 
					SAMLConstants.BINDING_SOAP);
			EndpointType redirectSLOEndpoint = selectEndpointByBinding(idpDef.getSingleLogoutServiceArray(),
					SAMLConstants.BINDING_HTTP_REDIRECT);
			EndpointType postSLOEndpoint = selectEndpointByBinding(idpDef.getSingleLogoutServiceArray(),
					SAMLConstants.BINDING_HTTP_POST);
			EndpointType soapSLOEndpoint = selectEndpointByBinding(idpDef.getSingleLogoutServiceArray(), 
					SAMLConstants.BINDING_SOAP);
			if (webEndpoint == null && soapEndpoint == null)
				continue;
			
			try
			{
				updatePKICerts(certs, entityId, SP_META_CERT);
			} catch (EngineException e)
			{
				log.error("Adding remote IDPs certs to local certs store failed, "
						+ "skipping IdP: " + entityId, e);
				continue;
			}
			UIInfoType uiInfo = parseMDUIInfo(idpDef.getExtensions(), entityId);
			Map<String, String> names = getLocalizedNames(uiInfo, idpDef, meta);
			Map<String, LogoType> logos = getLocalizedLogos(uiInfo);
			
			if (webEndpoint != null)
			{
				addEntryToProperties(entityId, webEndpoint, soapSLOEndpoint,
						postSLOEndpoint, redirectSLOEndpoint,
						requireSignedReq, realConfig, 
						configKey, properties, 1, certs, names, logos);
			}
			
			if (soapEndpoint != null)
			{
				addEntryToProperties(entityId, soapEndpoint, soapSLOEndpoint, 
						postSLOEndpoint, redirectSLOEndpoint,
						requireSignedReq, realConfig, 
						configKey, properties, 2, certs, names, logos);
			}
		}
	}
	
	private void addEntryToProperties(String entityId, EndpointType endpoint, 
			EndpointType sloSoapEndpoint, EndpointType sloPostEndpoint, EndpointType sloRedirectEndpoint,
			boolean requireSignedReq,
			SAMLSPProperties realConfig, String metaConfigKey, Properties properties, int index,
			List<X509Certificate> certs,
			Map<String, String> names, Map<String, LogoType> logos)
	{
		String configKey = getExistingKey(entityId, realConfig);
		String perMetaProfile = realConfig.getValue(metaConfigKey + 
				SAMLSPProperties.IDPMETA_TRANSLATION_PROFILE);
		String perMetaRegForm = realConfig.getValue(metaConfigKey + 
				SAMLSPProperties.IDPMETA_REGISTRATION_FORM);
	
		boolean noPerIdpConfig = configKey == null;
		String entityHex = DigestUtils.md5Hex(entityId);
		if (configKey == null)
			configKey = SAMLSPProperties.P + SAMLSPProperties.IDP_PREFIX + 
					"_entryFromMetadata_" + entityHex + "+" + index + "."; 

		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_ID))
			properties.setProperty(configKey + SAMLSPProperties.IDP_ID, 
					entityId);
		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_BINDING))
			properties.setProperty(configKey + SAMLSPProperties.IDP_BINDING, 
					convertBinding(endpoint.getBinding()));
		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_ADDRESS))
			properties.setProperty(configKey + SAMLSPProperties.IDP_ADDRESS, 
					endpoint.getLocation());
		setSLOProperty(properties, configKey, noPerIdpConfig, sloSoapEndpoint, 
				SamlProperties.SOAP_LOGOUT_URL, null);
		setSLOProperty(properties, configKey, noPerIdpConfig, sloPostEndpoint, 
				SamlProperties.POST_LOGOUT_URL, SamlProperties.POST_LOGOUT_RET_URL);
		setSLOProperty(properties, configKey, noPerIdpConfig, sloRedirectEndpoint, 
				SamlProperties.REDIRECT_LOGOUT_URL, SamlProperties.REDIRECT_LOGOUT_RET_URL);
		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_CERTIFICATE))
		{
			int i = 1;
			for (X509Certificate cert: certs)
			{
				if (!properties.containsKey(configKey + SAMLSPProperties.IDP_CERTIFICATES + i))
					properties.setProperty(configKey + SAMLSPProperties.IDP_CERTIFICATES + i, 
							getCertificateKey(cert, entityId, SP_META_CERT));
				i++;
			}
		}
		for (Map.Entry<String, String> name: names.entrySet())
		{
			String key = configKey + SAMLSPProperties.IDP_NAME + name.getKey();
			if (noPerIdpConfig || !properties.containsKey(key))
				properties.setProperty(key, name.getValue());
		}
		for (Map.Entry<String, LogoType> logo: logos.entrySet())
		{
			String key = configKey + SAMLSPProperties.IDP_LOGO + logo.getKey();
			if (noPerIdpConfig || !properties.containsKey(key))
				properties.setProperty(key, logo.getValue().getStringValue());
		}

		if (noPerIdpConfig || !properties.containsKey(configKey + SAMLSPProperties.IDP_SIGN_REQUEST))
			properties.setProperty(configKey + SAMLSPProperties.IDP_SIGN_REQUEST, 
					Boolean.toString(requireSignedReq));
		if ((perMetaProfile != null) && (noPerIdpConfig || 
				!properties.containsKey(configKey + CommonWebAuthnProperties.TRANSLATION_PROFILE)))
			properties.setProperty(configKey + CommonWebAuthnProperties.TRANSLATION_PROFILE, 
					perMetaProfile);
		if (perMetaRegForm != null && (noPerIdpConfig || 
				!properties.containsKey(configKey + CommonWebAuthnProperties.REGISTRATION_FORM)))
			properties.setProperty(configKey + CommonWebAuthnProperties.REGISTRATION_FORM, 
					perMetaRegForm);
		
		log.debug("Added a trusted IdP loaded from SAML metadata: " + entityId + " with " + 
				endpoint.getBinding() + " binding");
	}
		
	private String getExistingKey(String entityId, SAMLSPProperties realConfig)
	{
		Set<String> keys = realConfig.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		for (String key: keys)
		{
			if (entityId.equals(realConfig.getValue(key+SAMLSPProperties.IDP_ID)))
				return SAMLSPProperties.P + key;
		}
		return null;
	}
	
	private EndpointType selectWebEndpoint(EndpointType[] endpoints)
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
	
	private EndpointType selectEndpointByBinding(EndpointType[] endpoints, String requestedBinding)
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
	
	
	
}


