/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles import/export of identity types.
 * 
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeIE extends AbstractIEBase<IdentityType>
{
	public static final String IDENTITY_TYPE_OBJECT_TYPE = "identityTypes";
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, IdentityTypeIE.class);

	private final IdentityTypeDAO dbIdTypes;

	@Autowired
	public IdentityTypeIE(IdentityTypeDAO dbIdTypes, ObjectMapper objectMapper)
	{
		super(1, IDENTITY_TYPE_OBJECT_TYPE, objectMapper);
		this.dbIdTypes = dbIdTypes;
	}

	@Override
	protected List<IdentityType> getAllToExport()
	{
		return dbIdTypes.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(IdentityType exportedObj)
	{
		return jsonMapper.valueToTree(IdentityTypeMapper.map(exportedObj));
	}

	@Override
	protected void createSingle(IdentityType toCreate)
	{
		dbIdTypes.create(toCreate);
	}

	@Override
	protected IdentityType fromJsonSingle(ObjectNode src)
	{
		try
		{
			return IdentityTypeMapper.map(jsonMapper.treeToValue(src, DBIdentityType.class));

		} catch (JsonProcessingException e)
		{
			log.error("Failed to deserialize identity type object:", e);
		}

		return null;
	}
}
