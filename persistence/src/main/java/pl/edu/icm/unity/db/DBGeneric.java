/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.GenericMapper;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.WrongArgumentException;


/**
 * Interface allowing to manipulate generic content tables.
 * @author K. Benedyczak
 */
@Component
public class DBGeneric
{
	private DBLimits limits;
	
	@Autowired
	public DBGeneric(DB db)
	{
		this.limits = db.getDBLimits();
	}

	/**
	 * As {@link #addObject(String, String, String, byte[], SqlSession)} but last update is set to 
	 * the parameter value.
	 * @param name
	 * @param type
	 * @param subType
	 * @param contents
	 * @param lastUpdate
	 * @param sqlMap
	 * @return
	 * @throws WrongArgumentException
	 */
	public long addObject(String name, String type, String subType, byte[] contents, Date lastUpdate, 
			SqlSession sqlMap) throws WrongArgumentException
	{
		limits.checkNameLimit(name);
		limits.checkNameLimit(subType);
		if (contents == null)
			contents = new byte[0];
		limits.checkContentsLimit(contents);
		GenericObjectBean toAdd = new GenericObjectBean(name, contents, type);
		toAdd.setSubType(subType);
		toAdd.setLastUpdate(lastUpdate);
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		checkExists(toAdd, mapper, false);
		if (lastUpdate != null)
			mapper.insertObject2(toAdd);
		else
			mapper.insertObject(toAdd);
		return toAdd.getId();
	}
	
	public long addObject(String name, String type, String subType, byte[] contents, SqlSession sqlMap) 
			throws WrongArgumentException
	{
		return addObject(name, type, subType, contents, null, sqlMap);
	}

	public Set<String> getNamesOfType(String type, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		return mapper.selectObjectNamesByType(type);
	}
	
	public List<GenericObjectBean> getObjectsOfType(String type, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		return mapper.selectObjectsByType(type);
	}
	
	public GenericObjectBean getObjectByNameType(String name, String type, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		return mapper.selectObjectByNameType(new GenericObjectBean(name, null, type));
	}

	public void removeObject(String name, String type, SqlSession sqlMap)
			throws IllegalArgumentException
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		GenericObjectBean param = new GenericObjectBean(name, null, type);
		checkExists(param, mapper, true);
		mapper.deleteObjectByNameType(param);
	}
	
	public void removeObjectsByType(String type, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		mapper.deleteObjectsByType(type);
	}
	
	
	public void updateObject(String name, String type, byte[] contents, SqlSession sqlMap) 
			throws WrongArgumentException
	{
		limits.checkNameLimit(name);
		if (contents == null)
			contents = new byte[0];
		limits.checkContentsLimit(contents);
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		GenericObjectBean updated = new GenericObjectBean(name, contents, type);
		checkExists(updated, mapper, true);
		mapper.updateByNameType(updated);
	}
	
	private void checkExists(GenericObjectBean param, GenericMapper mapper, boolean shouldExist) 
			throws IllegalArgumentException
	{
		if (mapper.selectObjectByNameType(param) == null)
		{
			if (shouldExist)
				throw new IllegalArgumentException("The object with " + param.getName() 
					+ " name doesn't exist");
		} else
		{
			if (!shouldExist)
				throw new IllegalArgumentException("The object with " + param.getName() 
					+ " name already exists");
		}
	}
}



