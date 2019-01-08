/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.IdentityType;


/**
 * Implementation of {@link IdentityTypeSupport}, delegating to {@link IdentityHelper},
 * decorating it with transactions.
 * 
 * @author K. Benedyczak
 */
@Component
@Transactional
public class IdentityTypeSupportImpl implements IdentityTypeSupport
{
	@Autowired
	private IdentityTypeHelper helper;
	
	/**
	 * @param typeDef
	 * @return {@link IdentityTypeDefinition} for the given IdentityType.
	 */
	@Override
	public IdentityTypeDefinition getTypeDefinition(String idType)
	{
		return helper.getTypeDefinition(idType);
	}

	@Override
	public Collection<IdentityType> getIdentityTypes()
	{
		return helper.getIdentityTypes();
	}

	@Override
	public IdentityType getType(String idType)
	{
		return helper.getIdentityType(idType);
	}

	@Override
	public Map<String, IdentityTypeDefinition> getTypeDefinitionsMap()
	{
		Collection<IdentityType> identityTypes = getIdentityTypes();
		return identityTypes.stream().map(idType -> helper.getTypeDefinition(idType))
			.collect(Collectors.toMap(idDef -> idDef.getId(), idDef -> idDef));
	}
}
