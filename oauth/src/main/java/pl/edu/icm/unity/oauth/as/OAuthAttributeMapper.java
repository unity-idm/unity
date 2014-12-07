/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Maps Unity attributes to OAuth attributes, which is simple mapping to JSON.
 * @author K. Benedyczak
 */
public interface OAuthAttributeMapper
{
	public boolean isHandled(Attribute<?> unityAttribute);
	public Object getJsonValue(Attribute<?> unityAttribute);
	public String getJsonKey(Attribute<?> unityAttribute);
}
