/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.GenericObjectBean;

/**
 * Access to Generic.xml operations
 * @author K. Benedyczak
 */
public interface GenericMapper
{
	public int insertObject(GenericObjectBean toAdd);
	public int insertObject2(GenericObjectBean toAdd);
	public void deleteObjectByNameType(GenericObjectBean toRemove);
	public void deleteObjectsByType(String type);
	public void updateByNameType(GenericObjectBean updated);
	public List<GenericObjectBean> selectObjectsByType(String type);
	public List<String> selectObjectTypes();
	public GenericObjectBean selectObjectByNameType(GenericObjectBean nameType);
}
