/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Implementation of {@link AttributeSupport}
 * @author K. Benedyczak
 */
@Component
public class AttributeSupportImpl implements AttributeSupport
{
	private final AttributeTypeDAO aTypeDAO;
	private final AttributesHelper attributesHelper;
	private final AttributeDAO attributeDAO;
	
	@Autowired
	public AttributeSupportImpl(AttributesHelper attributesHelper, AttributeTypeDAO aTypeDAO, AttributeDAO attributeDAO)
	{
		this.attributesHelper = attributesHelper;
		this.aTypeDAO = aTypeDAO;
		this.attributeDAO = attributeDAO;
	}

	@Transactional
	@Override
	public AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId)
			throws EngineException
	{
		return attributesHelper.getAttributeTypeWithSingeltonMetadata(metadataId);
	}

	@Transactional
	@Override
	public List<AttributeType> getAttributeTypeWithMetadata(String metadataId)
			throws EngineException
	{
		Collection<AttributeType> existingAts = aTypeDAO.getAll();
		List<AttributeType> ret = new ArrayList<>();
		for (AttributeType at: existingAts)
			if (at.getMetadata().containsKey(metadataId))
				ret.add(at);
		return ret;
	}
	
	@Transactional
	@Override
	public AttributeExt getAttributeByMetadata(EntityParam entity, String group,
			String metadataId) throws EngineException
	{
		entity.validateInitialization();
		return attributesHelper.getAttributeByMetadata(entity, group, metadataId);
	}

	@Transactional
	@Override
	public Optional<String> getAttributeValueByMetadata(EntityParam entity, String group,
			String metadataId) throws EngineException
	{
		entity.validateInitialization();
		return Optional.ofNullable(attributesHelper.getAttributeValueByMetadata(entity, group, metadataId));
	}

	@Override
	@Transactional
	public Map<String, AttributeType> getAttributeTypesAsMap() throws EngineException
	{
		return aTypeDAO.getAllAsMap();
	}

	
	@Override
	@Transactional
	public Collection<Attribute> getAttributesByKeyword(String keyword)
	{
		return attributeDAO.getAllWithKeyword(keyword).stream()
				.map(StoredAttribute::getAttribute)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public Map<Long, List<Attribute>> getEntitiesWithAttributes(String attributeTypeName)
	{
		return attributeDAO.getAttributes(attributeTypeName, null, null).stream()
				.collect(Collectors.toMap(StoredAttribute::getEntityId,
							sa -> new ArrayList<>(Collections.singletonList(sa.getAttribute())),
							(oldV, newV) -> {oldV.addAll(newV);return oldV;}));
	}
}
