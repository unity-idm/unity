/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.util.function.Consumer;

import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Handles registration of metadata consumers and manages workers handling 
 * individual metadata retrievals.
 *  
 * @author K. Benedyczak
 */
public interface RemoteMetadataService
{
	public String registerConsumer(String url, long refreshIntervalMs,
			String customTruststore, Consumer<EntitiesDescriptorDocument> consumer);
	
	public void unregisterConsumer(String id);
}
