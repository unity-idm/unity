/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.basic.EntityInformation;

/**
 * Handles import/export of entities.
 * @author K. Benedyczak
 */
@Component
public class EntityIE extends AbstractIEBase<EntityInformation>
{
	public static final String ENTITIES_OBJECT_TYPE = "entities";
	
	private final EntityDAO dbIds;
	
	@Autowired
	public EntityIE(EntityDAO dbIds)
	{
		super(2, ENTITIES_OBJECT_TYPE);
		this.dbIds = dbIds;
	}

	@Override
	protected List<EntityInformation> getAllToExport()
	{
		return dbIds.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(EntityInformation exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(EntityInformation toCreate)
	{
		dbIds.createWithId(toCreate.getId(), toCreate);
	}

	@Override
	protected EntityInformation fromJsonSingle(ObjectNode src)
	{
		return new EntityInformation(src);
	}
}



