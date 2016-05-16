/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.store.export.DumpHeader;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Handles import/export of identities.
 * @author K. Benedyczak
 */
@Component
public class IdentityIE extends AbstractIEBase<Identity>
{
	@Autowired
	private IdentityDAO dbIds;
	@Autowired 
	private IdentityTypesRegistry idTypesRegistry;
	
	@Override
	protected List<Identity> getAllToExport()
	{
		return dbIds.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(Identity exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(Identity toCreate)
	{
		dbIds.create(toCreate);
	}

	@Override
	protected Identity fromJsonSingle(ObjectNode src, DumpHeader header)
	{
		if (header.getVersionMajor() < DumpHeader.V_INITIAL2)
			setComparableValue(src);
		return new Identity(src);
	}
	
	private void setComparableValue(ObjectNode src)
	{
		String type = src.get("typeId").asText();
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(type);
		String comparable;
		try
		{
			comparable = idTypeDef.getComparableValue(src.get("value").asText(), 
					src.get("realm").asText(null),
					src.get("target").asText(null));
		} catch (IllegalIdentityValueException e)
		{
			throw new InternalException("Can't deserialize identity: invalid value [" + 
					src.get("value") +"]", e);
		}
		src.put("comparableValue", comparable);
	}
}



