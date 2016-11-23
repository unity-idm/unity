/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.function.Supplier;

/**
 * Generic {@link AttributeValueSyntax} factory simplifying its creation. 
 * @author K. Benedyczak
 * @param <T>
 */
public abstract class AbstractAttributeValueSyntaxFactory<T> implements AttributeValueSyntaxFactory<T>
{
	private final String id;
	private final Supplier<AttributeValueSyntax<T>> instanceSupplier;
	
	public AbstractAttributeValueSyntaxFactory(String id, Supplier<AttributeValueSyntax<T>> instanceSupplier)
	{
		this.id = id;
		this.instanceSupplier = instanceSupplier;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public AttributeValueSyntax<T> createInstance()
	{
		return instanceSupplier.get();
	}
}
