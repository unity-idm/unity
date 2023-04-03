/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Handles import/export of entities.
 * @author K. Benedyczak
 */
@Component
public class AttributeIE extends AbstractIEBase<StoredAttributeWithKeywords>
{
	public static final String ATTRIBUTES_OBJECT_TYPE = "attributes";
	
	private final AttributeDAO dao;
	private final AttributeJsonSerializer serializer;
	
	@Autowired
	public AttributeIE(AttributeDAO dao, AttributeJsonSerializer serializer, ObjectMapper objectMapper)
	{
		super(6, ATTRIBUTES_OBJECT_TYPE, objectMapper);
		this.dao = dao;
		this.serializer = serializer;
	}

	@Override
	protected List<StoredAttributeWithKeywords> getAllToExport()
	{
		return dao.getAllIds().stream()
				.map(this::toStoredAttrWithKeywords)
				.collect(toList());
		
	}
	
	private StoredAttributeWithKeywords toStoredAttrWithKeywords(Long attributeId)
	{
		StoredAttribute storedAttribute = dao.getByKey(attributeId);
		List<String> keywords = dao.getAllKeywordsFor(attributeId);
		return new StoredAttributeWithKeywords(storedAttribute, keywords);
	}

	@Override
	protected ObjectNode toJsonSingle(StoredAttributeWithKeywords exportedObj)
	{
		ObjectNode attr = serializer.toJson(exportedObj.getStoredAttribute());
		ArrayNode keywords = jsonMapper.createArrayNode();
		exportedObj.getKeywords().forEach(keywords::add);
		attr.set("keywords", keywords);
		return attr;
	}

	@Override
	protected void createSingle(StoredAttributeWithKeywords toCreate)
	{
		long attributeId = dao.create(toCreate.getStoredAttribute());
		toCreate.getKeywords().forEach(keyword -> dao.linkKeywordToAttribute(keyword, attributeId));
	}

	@Override
	protected StoredAttributeWithKeywords fromJsonSingle(ObjectNode src)
	{
		List<String> keywords = Lists.newArrayList();
		if (JsonUtil.notNull(src, "keywords"))
		{
			ArrayNode keywordsArray = (ArrayNode) src.remove("keywords");
			keywordsArray.forEach(keywordNode -> keywords.add(keywordNode.asText()));
		}
		StoredAttribute storedAttribute = serializer.fromJson(src);
		return new StoredAttributeWithKeywords(storedAttribute, keywords);
	}
}



