/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.store.types.StoredIdentity;

/**
 * Handles import/export of identities.
 * @author K. Benedyczak
 */
@Component
public class IdentityIE extends AbstractIEBase<StoredIdentity>
{
	public static final String IDENTITIES_OBJECT_TYPE = "identities";
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, IdentityIE.class);
	
	private final IdentityDAO dbIds;
	
	@Autowired
	public IdentityIE(IdentityDAO dbIds, ObjectMapper objectMapper)
	{
		super(3, IDENTITIES_OBJECT_TYPE, objectMapper);
		this.dbIds = dbIds;
	}

	@Override
	protected List<StoredIdentity> getAllToExport()
	{
		return dbIds.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(StoredIdentity exportedObj)
	{
		return jsonMapper.valueToTree(IdentityMapper.map(exportedObj.getIdentity()));
	}

	@Override
	protected void createSingle(StoredIdentity toCreate)
	{
		if (toCreate != null)
			dbIds.create(toCreate);
	}

	@Override
	protected StoredIdentity fromJsonSingle(ObjectNode src)
	{
		try
		{
			return new StoredIdentity(IdentityMapper.map(jsonMapper.treeToValue(src, DBIdentity.class)));
		} catch (IllegalArgumentException e)
		{
			if (log.isDebugEnabled())
				log.debug("Skipping identity without comaprable value: likely transient.", e);
			else
				log.info("Skipping identity without comaprable value: likely transient. " + src);
		} catch (JsonProcessingException e)
		{
			log.error("Failed to deserialize Identity object:", e);
		}
		
		return null;
	}
}



