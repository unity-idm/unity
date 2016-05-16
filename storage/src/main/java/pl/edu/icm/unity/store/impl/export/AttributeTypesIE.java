/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.export;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Handles import/export of attribute types table.
 * @author K. Benedyczak
 */
@Component
public class AttributeTypesIE extends AbstractIEBase<AttributeType>
{
	@Autowired
	private AttributeTypeDAO dbAttributes;
	
	@Override
	protected List<AttributeType> getAllToExport()
	{
		return dbAttributes.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(AttributeType exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(AttributeType toCreate)
	{
		dbAttributes.create(toCreate);
	}

	@Override
	protected AttributeType fromJsonSingle(ObjectNode src)
	{
		return new AttributeType(src);
	}
}








