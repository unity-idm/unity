/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

/**
 * Implementation are intended to be app singletons, and should provide instances of attribute handlers.
 * @author K. Benedyczak
 */
public interface WebAttributeHandlerFactory
{
	String getSupportedSyntaxId();
	
	WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax);
	
	/**
	 * @param initialValue value to be edited or null if value is to be created from scratch
	 * @return
	 */
	AttributeSyntaxEditor<?> getSyntaxEditorComponent(AttributeValueSyntax<?> syntax);
}
