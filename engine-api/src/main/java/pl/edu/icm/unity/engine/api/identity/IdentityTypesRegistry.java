/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.registries.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link IdentityTypeDefinition}s.
 * 
 * @author K. Benedyczak
 */
@Component("IdentityTypesRegistry")
public class IdentityTypesRegistry extends TypesRegistryBase<IdentityTypeDefinition>
{
	private Collection<IdentityTypeDefinition> dynamic;
	
	@Autowired
	public IdentityTypesRegistry(Optional<List<IdentityTypeDefinition>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
		dynamic = new ArrayList<IdentityTypeDefinition>();
		for (IdentityTypeDefinition id: getAll())
		{
			if (id.isDynamic())
				dynamic.add(id);
		}
		dynamic = Collections.unmodifiableList((List<? extends IdentityTypeDefinition>)dynamic);
	}

	@Override
	protected String getId(IdentityTypeDefinition from)
	{
		return from.getId();
	}
	
	public Collection<IdentityTypeDefinition> getDynamic()
	{
		return dynamic;
	}
}
