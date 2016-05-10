/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;


/**
 * RDBMS storage of {@link GenericObjectBean}
 * @author K. Benedyczak
 */
@Repository(GenericRDBMSStore.BEAN)
public class GenericRDBMSStore extends GenericRDBMSCRUD<GenericObjectBean, GenericObjectBean> 
					implements ObjectStoreDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public GenericRDBMSStore(GenericObjectRDBMSSerializer serializer)
	{
		super(GenericMapper.class, serializer, NAME);
	}


	@Override
	public void removeObject(String name, String type)
	{
		GenericObjectBean toRemove = getObjectByNameType(name, type);
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		mapper.deleteByKey(toRemove.getId());
	}

	@Override
	public void removeObjectsByType(String type)
	{
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		mapper.deleteByType(type);
	}

	@Override
	public void updateObject(String name, String type, byte[] contents)
	{
		GenericObjectBean toUpdate = getObjectByNameType(name, type);
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		toUpdate.setContents(contents);
		toUpdate.setLastUpdate(new Date());
		mapper.updateByKey(toUpdate);
	}
	
	@Override
	public Set<String> getNamesOfType(String type)
	{
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		return mapper.selectObjectNamesByType(type);
	}

	@Override
	public List<GenericObjectBean> getObjectsOfType(String type)
	{
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		return mapper.selectObjectsByType(type);
	}

	@Override
	public GenericObjectBean getObjectByNameType(String name, String type)
	{
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		
		GenericObjectBean ret = mapper.selectObjectByNameType(new GenericObjectBean(name, type));
		if (ret == null)
			throw new IllegalArgumentException(elementName + " with key [" + name + "//" + type + 
					"] does not exist");
		return ret;
	}

	@Override
	public Set<String> getObjectTypes()
	{
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		return mapper.selectObjectTypes();
	}
}
