/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.identity.EntityInformation;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;

/**
 * Handles import/export of entities.
 * @author K. Benedyczak
 */
@Component
public class EntityIE extends AbstractIEBase<EntityInformation>
{
	public static final String ENTITIES_OBJECT_TYPE = "entities";
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, EntityIE.class);	

	private final EntityDAO dbIds;
	
	@Autowired
	public EntityIE(EntityDAO dbIds, ObjectMapper objectMapper)
	{
		super(2, ENTITIES_OBJECT_TYPE, objectMapper);
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
		return jsonMapper.valueToTree(EntityInformationMapper.map(exportedObj));
	}

	@Override
	protected void createSingle(EntityInformation toCreate)
	{
		dbIds.createWithId(toCreate.getId(), toCreate);
	}

	@Override
	protected EntityInformation fromJsonSingle(ObjectNode src)
	{
		try
		{
			return EntityInformationMapper.map(jsonMapper.treeToValue(src, DBEntityInformation.class));
		} catch (JsonProcessingException e)
		{
			log.error("Failed to deserialize EntityInformation object:", e);
		}
		return null;
	}
}



