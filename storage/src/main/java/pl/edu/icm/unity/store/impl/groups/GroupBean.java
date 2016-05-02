/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import pl.edu.icm.unity.store.rdbms.BaseBean;


/**
 * In DB group representation.
 * @author K. Benedyczak
 */
public class GroupBean extends BaseBean
{
	private String parentPath;
	
	public GroupBean() 
	{
	}
	
	public GroupBean(String path, String parentPath) 
	{
		this.parentPath = parentPath;
		setName(path);
	}
	
	public String getParent()
	{
		return parentPath;
	}
	public void setParent(String parentPath)
	{
		this.parentPath = parentPath;
	}
}
