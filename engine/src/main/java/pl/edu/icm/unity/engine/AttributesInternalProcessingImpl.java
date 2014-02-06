/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of {@link AttributesInternalProcessing}
 * @author K. Benedyczak
 */
@Component
public class AttributesInternalProcessingImpl implements AttributesInternalProcessing
{
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	private AttributesHelper attributesHelper;
	
	@Autowired
	public AttributesInternalProcessingImpl(DBSessionManager db, 
			DBAttributes dbAttributes, AttributesHelper attributesHelper)
	{
		this.db = db;
		this.dbAttributes = dbAttributes;
		this.attributesHelper = attributesHelper;
	}

	@Override
	public AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId)
			throws EngineException
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			AttributeType ret = attributesHelper.getAttributeTypeWithSingeltonMetadata(metadataId, sql);
			sql.close();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public List<AttributeType> getAttributeTypeWithMetadata(String metadataId)
			throws EngineException
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			Collection<AttributeType> existingAts = dbAttributes.getAttributeTypes(sql).values();
			List<AttributeType> ret = new ArrayList<>();
			for (AttributeType at: existingAts)
				if (at.getMetadata().containsKey(metadataId))
					ret.add(at);
			sql.close();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	
	@Override
	public AttributeExt<?> getAttributeByMetadata(EntityParam entity, String group,
			String metadataId) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(false);
		try
		{
			AttributeExt<?> retA = attributesHelper.getAttributeByMetadata(entity, group, metadataId, sql);
			sql.close();
			return retA;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
}
