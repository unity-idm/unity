/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Base code for import/export of generic objects. Typical extension should only fix the generic param
 * and properly call super-constructor.
 * @author K. Benedyczak
 */
public class GenericObjectIEBase<T extends NamedObject> extends AbstractIEBase<T>
{
	private GenericObjectsDAO<T> dao;
	private ObjectMapper jsonMapper;
	private Class<T> clazz;
	
	public GenericObjectIEBase(GenericObjectsDAO<T> dao, ObjectMapper jsonMapper, Class<T> clazz)
	{
		this.dao = dao;
		this.jsonMapper = jsonMapper;
		this.clazz = clazz;
	}

	@Override
	protected List<T> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(T exportedObj)
	{
		return jsonMapper.convertValue(exportedObj, ObjectNode.class);
	}

	@Override
	protected void createSingle(T toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected T fromJsonSingle(ObjectNode src)
	{
		return jsonMapper.convertValue(src, clazz);
	}
}



