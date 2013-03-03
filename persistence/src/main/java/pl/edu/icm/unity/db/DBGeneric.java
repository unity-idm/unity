/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.GenericMapper;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.GenericObjectBean;


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

	public long addObject(String name, String type, byte[] contents, SqlSession sqlMap)
	{
		limits.checkNameLimit(name);
		if (contents == null)
			contents = new byte[0];
		limits.checkContentsLimit(contents);
		GenericObjectBean toAdd = new GenericObjectBean(name, contents, type);
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		mapper.insertObject(toAdd);
		return toAdd.getId();
	}
	
	public List<GenericObjectBean> getObjectsOfType(String type, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		return mapper.selectObjectsByType(type);
	}
	
	public GenericObjectBean getObjectById(long id, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		return mapper.selectObjectById(id);
	}

	public void removeObject(long id, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		mapper.deleteObject(id);
	}
	
	public void removeObjectsByType(String type, SqlSession sqlMap)
	{
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		mapper.deleteObjectsByType(type);
	}
	
	
	public void updateObject(long id, String name, String type, byte[] contents, SqlSession sqlMap)
	{
		limits.checkNameLimit(name);
		if (contents == null)
			contents = new byte[0];
		limits.checkContentsLimit(contents);
		GenericMapper mapper = sqlMap.getMapper(GenericMapper.class);
		GenericObjectBean updated = new GenericObjectBean(name, contents, type);
		updated.setId(id);
		mapper.updateById(updated);
	}
}



