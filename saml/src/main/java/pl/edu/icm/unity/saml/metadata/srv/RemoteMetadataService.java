/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.time.Duration;
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
	
	void registerConsumer(String key, Duration refreshInterval,
			String customTruststore, BiConsumer<EntitiesDescriptorDocument, String> consumer, boolean logoDownload);
	
	void unregisterConsumer(String id);
	
	void reset();
}
