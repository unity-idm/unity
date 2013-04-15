/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

/**
 * Implementation are intended to be app singletons, and should provide instances of attribute handlers.
 * @author K. Benedyczak
 */
public interface WebAttributeHandlerFactory
{
	public String getSupportedSyntaxId();
	
	public WebAttributeHandler<?> createInstance();
}
