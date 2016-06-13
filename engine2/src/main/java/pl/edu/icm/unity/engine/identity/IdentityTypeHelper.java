/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
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
	private IdentityDAO identityDAO;
	private IdentityTypesRegistry idImplRegistry;
	private IdentityTypeDAO idTypeDAO;
	
	/**
	 * Creates a full {@link Identity} object from simple {@link IdentityParam} 
	 * using {@link IdentityType} of the argument.
	 * 
	 * @param toUpcast
	 * @return
	 * @throws IllegalIdentityValueException 
	 */
	public Identity upcastIdentityParam(IdentityParam toUpcast) throws IllegalIdentityValueException
	{
		IdentityTypeDefinition idDef = idImplRegistry.getByName(toUpcast.getTypeId());
		return identityDAO.get(idDef.getComparableValue(toUpcast.getValue(), 
				toUpcast.getRealm(), toUpcast.getTarget()));
	}
	
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
	public IdentityTypeDefinition getTypeDefinition(String idType)
	{
		return getTypeDefinition(getIdentityType(idType));
	}
}
