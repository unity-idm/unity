/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

/**
 * Implementations allow to instantiate {@link AttributeValueSyntax} instances.
 * 
 * @author K. Benedyczak
 */
public interface AttributeValueSyntaxFactory<T>
{
	public AttributeValueSyntax<T> createInstance();
	public String getId();
}
