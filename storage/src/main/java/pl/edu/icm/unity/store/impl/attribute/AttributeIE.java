/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Handles import/export of entities.
 * @author K. Benedyczak
 */
@Component
public class AttributeIE extends AbstractIEBase<StoredAttribute>
{
	public static final String ATTRIBUTES_OBJECT_TYPE = "attributes";
	
	private AttributeDAO dao;
	private AttributeJsonSerializer serializer;
	
	@Autowired
	public AttributeIE(AttributeDAO dao, AttributeJsonSerializer serializer)
	{
		super(6, ATTRIBUTES_OBJECT_TYPE);
		this.dao = dao;
		this.serializer = serializer;
	}

	@Override
	protected List<StoredAttribute> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(StoredAttribute exportedObj)
	{
		return serializer.toJson(exportedObj);
	}

	@Override
	protected void createSingle(StoredAttribute toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected StoredAttribute fromJsonSingle(ObjectNode src)
	{
		return serializer.fromJson(src);
	}
}



