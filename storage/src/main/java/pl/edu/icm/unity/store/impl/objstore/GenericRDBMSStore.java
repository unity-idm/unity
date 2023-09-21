/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

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
	public static final String BEAN = ObjectStoreDAO.DAO_ID + "rdbms";

	@Autowired
	public GenericRDBMSStore(GenericObjectRDBMSSerializer serializer)
	{
		super(GenericMapper.class, serializer, NAME);
	}


	@Override
	public void removeObject(String name, String type)
	{
		GenericObjectBean toRemove = getObjectByNameType(name, type);
		if (toRemove == null)
			throw new IllegalArgumentException("Trying to remove not existing object [" 
					+ name + "//" + type + "]");
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
	public void updateObject(String name, String type, GenericObjectBean obj)
	{
		GenericObjectBean toUpdate = getObjectByNameType(name, type);
		if (toUpdate == null)
			throw new IllegalArgumentException("Trying to update not existing object [" 
					+ name + "//" + type + "]");
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		toUpdate.setContents(obj.getContents());
		toUpdate.setName(obj.getName());
		toUpdate.setLastUpdate(obj.getLastUpdate());
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
		
		return mapper.selectObjectByNameType(new GenericObjectBean(name, type));
	}

	@Override
	public Set<String> getObjectTypes()
	{
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		return mapper.selectObjectTypes();
	}


	@Override
	public long getCountByType(String type)
	{
		GenericMapper mapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		return mapper.getCountByType(type);
	}
}
