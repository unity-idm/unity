/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore;

import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Base code for import/export of generic objects. Typical extension should only fix the generic param
 * and properly call super-constructor.
 * @author K. Benedyczak
 */
public abstract class GenericObjectIEBase<T extends NamedObject> extends AbstractIEBase<Entry<T, Date>>
{
	private NamedCRUDDAOWithTS<T> dao;
	
	
	public GenericObjectIEBase(NamedCRUDDAOWithTS<T> dao, ObjectMapper jsonMapper,
			int sortKey, String name)
	{
		super(sortKey, name, jsonMapper);
		this.dao = dao;
		
		
	}

	@Override
	protected List<Entry<T, Date>> getAllToExport()
	{
		return dao.getAllWithUpdateTimestamps();
	}

	@Override
	protected ObjectNode toJsonSingle(Entry<T, Date> exportedObj)
	{
		ObjectNode wrapper = jsonMapper.createObjectNode();
		ObjectNode obj = convert(exportedObj.getKey());
		wrapper.put("_updateTS", exportedObj.getValue().getTime());
		wrapper.set("obj", obj);
		return wrapper;
	}

	@Override
	protected void createSingle(Entry<T, Date> toCreate)
	{
		
		dao.createWithTS(toCreate.getKey(), toCreate.getValue());
	}

	@Override
	protected Entry<T, Date> fromJsonSingle(ObjectNode src)
	{
		Date updateTS = new Date(src.get("_updateTS").asLong());
		ObjectNode obj = (ObjectNode) src.get("obj");
		T val = convert(obj);
		return new SimpleEntry<T, Date>(val, updateTS);
	}
	
	protected abstract T convert(ObjectNode src);
	protected abstract ObjectNode convert(T src);
}



