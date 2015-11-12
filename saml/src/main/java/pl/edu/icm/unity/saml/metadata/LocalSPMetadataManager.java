/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SLOSPManager;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;

/**
 * Responsible for configuration and loading of local metadata for authenticators (SP).
 * @author K. Benedyczak
 */
public class LocalSPMetadataManager
{
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	private SLOSPManager sloManager;
	private SLOReplyInstaller sloReplyInstaller;
	private MultiMetadataServlet metadataServlet;
	
	private MetadataProvider provider;

	public LocalSPMetadataManager(ExecutorsService executorsService,  
			String responseConsumerAddress, SLOSPManager sloManager, SLOReplyInstaller sloReplyInstaller,
			MultiMetadataServlet metadataServlet)
	{
		this.executorsService = executorsService;
		this.responseConsumerAddress = responseConsumerAddress;
		this.sloManager = sloManager;
		this.sloReplyInstaller = sloReplyInstaller;
		this.metadataServlet = metadataServlet;
	}
	
	public synchronized void updateConfiguration(SAMLSPProperties samlProperties)
	{
		String metaPath = "/" + samlProperties.getValue(SAMLSPProperties.METADATA_PATH);
		if (this.provider != null)
		{
			this.provider.stop();
			this.provider = null;
		}

		if (samlProperties.getBooleanValue(SamlProperties.PUBLISH_METADATA))
		{
			MetadataProvider newProvider = createNewProvider(samlProperties);
			this.provider = newProvider;
			metadataServlet.updateProvider(metaPath, newProvider);
		} else
		{
			metadataServlet.removeProvider(metaPath);
		}
	}
	
	private MetadataProvider createNewProvider(SAMLSPProperties samlProperties)
	{
		IndexedEndpointType consumerEndpoint = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint.setIndex(1);
		consumerEndpoint.setBinding(SAMLConstants.BINDING_HTTP_POST);
		consumerEndpoint.setLocation(responseConsumerAddress);
		consumerEndpoint.setIsDefault(true);

		IndexedEndpointType consumerEndpoint2 = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint2.setIndex(2);
		consumerEndpoint2.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);
		consumerEndpoint2.setLocation(responseConsumerAddress);
		consumerEndpoint2.setIsDefault(false);

		EndpointType[] sloEndpoints = null;
		String sloPath = samlProperties.getValue(SAMLSPProperties.SLO_PATH);
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
		
		IndexedEndpointType[] assertionConsumerEndpoints = new IndexedEndpointType[] {consumerEndpoint,
				consumerEndpoint2};
		return MetadataProviderFactory.newSPInstance(samlProperties, 
				executorsService, assertionConsumerEndpoints, sloEndpoints);
	}
}
