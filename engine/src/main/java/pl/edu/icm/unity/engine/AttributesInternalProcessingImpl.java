/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
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
@Transactional
public class AttributesInternalProcessingImpl implements AttributesInternalProcessing
{
	private DBAttributes dbAttributes;
	private AttributesHelper attributesHelper;
	
	@Autowired
	public AttributesInternalProcessingImpl(DBAttributes dbAttributes, AttributesHelper attributesHelper)
	{
		this.dbAttributes = dbAttributes;
		this.attributesHelper = attributesHelper;
	}

	@Override
	public AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId)
			throws EngineException
	{
		return attributesHelper.getAttributeTypeWithSingeltonMetadata(metadataId, 
					SqlSessionTL.sqlSession.get());
	}

	@Override
	public List<AttributeType> getAttributeTypeWithMetadata(String metadataId)
			throws EngineException
	{
		Collection<AttributeType> existingAts = dbAttributes.getAttributeTypes(
				SqlSessionTL.sqlSession.get()).values();
		List<AttributeType> ret = new ArrayList<>();
		for (AttributeType at: existingAts)
			if (at.getMetadata().containsKey(metadataId))
				ret.add(at);
		return ret;
	}
	
	
	@Override
	public AttributeExt<?> getAttributeByMetadata(EntityParam entity, String group,
			String metadataId) throws EngineException
	{
		entity.validateInitialization();
		return attributesHelper.getAttributeByMetadata(entity, group, metadataId, 
					SqlSessionTL.sqlSession.get());
	}
}
