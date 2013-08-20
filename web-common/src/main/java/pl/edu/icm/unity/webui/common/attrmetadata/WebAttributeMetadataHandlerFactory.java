/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attrmetadata;

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
