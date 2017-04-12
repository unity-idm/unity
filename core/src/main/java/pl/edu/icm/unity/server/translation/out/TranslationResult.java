/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.DynamicAttribute;

/**
 * Result of output translation. Set of identities and attributes. This class is mutable: actions modify the contents
 * one by one.
 * @author K. Benedyczak
 */
public class TranslationResult
{
	private Collection<DynamicAttribute> attributes = new HashSet<>();
	private Collection<IdentityParam> identities = new ArrayList<>();

	private Collection<Attribute<?>> attributesToPersist = new HashSet<>();
	private Collection<IdentityParam> identitiesToPersist = new ArrayList<>();
	
	public Collection<DynamicAttribute> getAttributes()
	{
		return attributes;
	}
	
	public Collection<IdentityParam> getIdentities()
	{
		return identities;
	}

	public Collection<Attribute<?>> getAttributesToPersist()
	{
		return attributesToPersist;
	}

	public Collection<IdentityParam> getIdentitiesToPersist()
	{
		return identitiesToPersist;
	}
}
