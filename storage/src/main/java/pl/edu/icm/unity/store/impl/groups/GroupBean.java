/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.Objects;

import pl.edu.icm.unity.store.rdbms.BaseBean;


/**
 * In DB group representation.
 */
public class GroupBean extends BaseBean
{
	private Integer parentId;
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
	public Integer getParentId()
	{
		return parentId;
	}
	public void setParentId(Integer parentId)
	{
		this.parentId = parentId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(parentId, parentPath);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroupBean other = (GroupBean) obj;
		return Objects.equals(parentId, other.parentId) && Objects.equals(parentPath, other.parentPath);
	}
}
