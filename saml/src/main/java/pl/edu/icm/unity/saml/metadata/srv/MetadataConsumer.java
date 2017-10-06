/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.util.function.Consumer;

import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Information about remote metadata consumer
 * 
 * @author K. Benedyczak
 */
class MetadataConsumer
{
	final long refreshInterval;
	final Consumer<EntitiesDescriptorDocument> consumer;
	final String id;

	public MetadataConsumer(long refreshInterval,
			Consumer<EntitiesDescriptorDocument> consumer, String id)
	{
		this.refreshInterval = refreshInterval;
		this.consumer = consumer;
		this.id = id;
	}
}
