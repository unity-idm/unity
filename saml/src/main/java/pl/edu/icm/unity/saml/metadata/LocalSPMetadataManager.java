/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.sp.SLOSPManager;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;

/**
 * Responsible for configuration and loading of local metadata for authenticators (SP).
 * @author K. Benedyczak
 */
public class LocalSPMetadataManager
{
	private final ExecutorsService executorsService;
	private final String responseConsumerAddress;
	private final SLOSPManager sloManager;
	private final SLOReplyInstaller sloReplyInstaller;
	private final MultiMetadataServlet metadataServlet;
	private final URIAccessService uriAccessService;
	
	private MetadataProvider provider;

	public LocalSPMetadataManager(ExecutorsService executorsService,  
			String responseConsumerAddress, SLOSPManager sloManager, SLOReplyInstaller sloReplyInstaller,
			MultiMetadataServlet metadataServlet, URIAccessService uriAccessService)
	{
		this.executorsService = executorsService;
		this.responseConsumerAddress = responseConsumerAddress;
		this.sloManager = sloManager;
		this.sloReplyInstaller = sloReplyInstaller;
		this.metadataServlet = metadataServlet;
		this.uriAccessService = uriAccessService;
	}
	
	public synchronized void updateConfiguration(SAMLSPConfiguration samlConfiguration)
	{
		String metaPath = "/" + samlConfiguration.metadataURLPath;
		if (this.provider != null)
		{
			this.provider.stop();
			this.provider = null;
		}

		if (samlConfiguration.publishMetadata)
		{
			MetadataProvider newProvider = createNewProvider(samlConfiguration);
			this.provider = newProvider;
			metadataServlet.updateProvider(metaPath, newProvider);
		} else
		{
			metadataServlet.removeProvider(metaPath);
		}
	}
	
	private MetadataProvider createNewProvider(SAMLSPConfiguration samlConfiguration)
	{
		IndexedEndpointType consumerEndpoint = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint.setIndex(1);
		consumerEndpoint.setBinding(SAMLConstants.BINDING_HTTP_POST);
		consumerEndpoint.setLocation(responseConsumerAddress);
		consumerEndpoint.setIsDefault(true);

		EndpointType[] sloEndpoints = null;
		String sloPath = samlConfiguration.sloPath;
		String sloEndpointURL = sloPath != null ? sloManager.getAsyncServletURL(sloPath) : null;
		String sloSoapPath = sloPath != null ? sloManager.getSyncServletURL(sloPath) : null; 
		if (sloEndpointURL != null && sloSoapPath != null)
		{
			EndpointType sloPost = EndpointType.Factory.newInstance();
			sloPost.setLocation(sloEndpointURL);
			sloPost.setBinding(SAMLConstants.BINDING_HTTP_POST);
			sloPost.setResponseLocation(sloReplyInstaller.getServletURL());
			
			EndpointType sloRedirect = EndpointType.Factory.newInstance();
			sloRedirect.setLocation(sloEndpointURL);
			sloRedirect.setResponseLocation(sloReplyInstaller.getServletURL());
			sloRedirect.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);
			
			EndpointType sloSoap = EndpointType.Factory.newInstance();
			sloSoap.setLocation(sloSoapPath);
			sloSoap.setBinding(SAMLConstants.BINDING_SOAP);
			
			sloEndpoints = new EndpointType[] {sloPost, sloRedirect, sloSoap};
		}
		
		IndexedEndpointType[] assertionConsumerEndpoints = new IndexedEndpointType[] {consumerEndpoint};
		return MetadataProviderFactory.newSPInstance(samlConfiguration, uriAccessService,
				executorsService, assertionConsumerEndpoints, sloEndpoints);
	}
}
