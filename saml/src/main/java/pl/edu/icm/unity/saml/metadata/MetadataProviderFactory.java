/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.types.I18nString;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;

/**
 * Utility class simplifying creation of {@link MetadataProvider}s.
 * @author K. Benedyczak
 */
public class MetadataProviderFactory
{
	public static MetadataProvider newIdpInstance(SamlIdpProperties samlProperties, URIAccessService uriAccessService,
	                                              ExecutorsService executorsService, EndpointType[] ssoEndpoints,
	                                              EndpointType[] attributeQueryEndpoints, EndpointType[] sloEndpoints,
	                                              I18nString displayedName, MessageSource msg)
	{
		MetadataProvider metaProvider;		
		String uri = samlProperties.getValue(SamlProperties.METADATA_SOURCE);
		
		if (uri == null)
		{
			metaProvider = new IdpMetadataGenerator(samlProperties, ssoEndpoints, 
					attributeQueryEndpoints, sloEndpoints, displayedName, msg);
		} else
		{
			try
			{
				metaProvider = new URIMetadataProvider(executorsService, uriAccessService, uri);
			} catch (EngineException e)
			{
				throw new ConfigurationException("Can't initialize metadata provider, " +
						"problem loading metadata", e);
			}
		}
		boolean signMeta = samlProperties.getBooleanValue(SamlProperties.SIGN_METADATA);
		return signMeta ? 
				addSigner(metaProvider, samlProperties.getSamlIssuerCredential()) : 
				metaProvider;
	}

	public static MetadataProvider newSPInstance(SAMLSPConfiguration samlConfiguration, URIAccessService uriAccessService,
			ExecutorsService executorsService, IndexedEndpointType[] assertionConsumerEndpoints, 
			EndpointType[] sloEndpoints)
	{
		MetadataProvider metaProvider;
		String uri = samlConfiguration.ourMetadataFilePath;
		if (uri == null)
		{
			metaProvider = new SPMetadataGenerator(samlConfiguration, assertionConsumerEndpoints,
					sloEndpoints);
		} else
		{
			try
			{
				metaProvider = new URIMetadataProvider(executorsService, uriAccessService, uri);
			} catch (EngineException e)
			{
				throw new ConfigurationException("Can't initialize metadata provider, " +
						"problem loading metadata", e);
			}
		}
		return samlConfiguration.signPublishedMetadata ? 
				addSigner(metaProvider, samlConfiguration.requesterCredential) : 
				metaProvider;
	}

	
	private static MetadataProvider addSigner(MetadataProvider metaProvider, X509Credential credential)
	{
		try
		{
			return new MetadataSigner(metaProvider, credential);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize metadata provider, " +
					"problem signing metadata", e);
		}
	}
}
