/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.time.Duration;
import java.util.function.BiConsumer;

import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Information about remote metadata consumer
 * 
 * @author K. Benedyczak
 */
class MetadataConsumer
{
	final Duration refreshInterval;
	final BiConsumer<EntitiesDescriptorDocument, String> consumer;
	final String id;

	public MetadataConsumer(Duration refreshInterval,
			BiConsumer<EntitiesDescriptorDocument, String> consumer, String id)
	{
		this.refreshInterval = refreshInterval;
		this.consumer = consumer;
		this.id = id;
	}
}
