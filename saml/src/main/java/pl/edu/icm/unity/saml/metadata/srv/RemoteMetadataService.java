/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.util.function.BiConsumer;

import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Handles registration of metadata consumers and manages workers handling 
 * individual metadata retrievals.
 *  
 * @author K. Benedyczak
 */
public interface RemoteMetadataService
{
	String preregisterConsumer(String url);
	
	void registerConsumer(String key, long refreshIntervalMs,
			String customTruststore, BiConsumer<EntitiesDescriptorDocument, String> consumer);
	
	void unregisterConsumer(String id);
	
	void reset();
}
