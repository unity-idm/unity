/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;


/**
 * Helper operations simplifying relation of {@link Identity} with their corresponding {@link IdentityType}.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeHelper
{
	private IdentityTypesRegistry idImplRegistry;
	private IdentityTypeDAO idTypeDAO;
	
	
	@Autowired
	public IdentityTypeHelper(IdentityTypesRegistry idImplRegistry, IdentityTypeDAO idTypeDAO)
	{
		this.idImplRegistry = idImplRegistry;
		this.idTypeDAO = idTypeDAO;
	}

	/**
	 * Creates a full {@link Identity} object from simple {@link IdentityParam} 
	 * using {@link IdentityType} of the argument. The returned identity is assumed to be a new one, 
	 * i.e. this operation is not resolving the existing Identity using the given parameter.
	 * 
	 * @param toUpcast
	 * @return
	 * @throws IllegalIdentityValueException 
	 */
	public Identity upcastIdentityParam(IdentityParam toUpcast, long entityId) throws IllegalIdentityValueException
	{
		IdentityTypeDefinition idDef = idImplRegistry.getByName(toUpcast.getTypeId());
		String comparableValue = idDef.getComparableValue(toUpcast.getValue(), 
				toUpcast.getRealm(), toUpcast.getTarget()); 
		return new Identity(toUpcast, entityId, comparableValue);
	}
	
	@Transactional
	public IdentityType getIdentityType(String idType)
	{
		return idTypeDAO.get(idType);
	}
	
	/**
	 * @param typeDef
	 * @return {@link IdentityTypeDefinition} for the given IdentityType.
	 */
	public IdentityTypeDefinition getTypeDefinition(IdentityType idType)
	{
		return idImplRegistry.getByName(idType.getIdentityTypeProvider());
	}

	/**
	 * @param typeDef
	 * @return {@link IdentityTypeDefinition} for the given IdentityType.
	 */
	@Transactional
	public IdentityTypeDefinition getTypeDefinition(String idType)
	{
		return getTypeDefinition(getIdentityType(idType));
	}
	
	@Transactional
	public Collection<IdentityType> getIdentityTypes()
	{
		return idTypeDAO.getAll();
	}
}
