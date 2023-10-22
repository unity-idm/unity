/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.metadata;

/**
 * Factory of {@link WebAttributeMetadataHandler}s.
 * 
 * @author K. Benedyczak
 */
public interface WebAttributeMetadataHandlerFactory
{
	public String getSupportedMetadata();
	
	public WebAttributeMetadataHandler newInstance();
}
