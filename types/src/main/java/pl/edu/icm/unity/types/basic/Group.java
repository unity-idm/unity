/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 14, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.types.basic;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Group holds set of other elements: other groups and identities. This class only denotes group, 
 * it doesn't hold group's content.
 * 
 * @author K. Benedyczak
 */
public class Group implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private String[] path;
	private String description = "";
	private AttributeStatement[] attributeStatements = new AttributeStatement[0];
	
	public Group(Group parent, String name)
	{
		if (name == null || name.equals("") || name.contains("/"))
			throw new IllegalArgumentException("Group name must be a non empty string without '/' character");
		String parentP[] = parent.getPath();
		path = new String[parentP.length + 1];
		for (int i=0; i<parentP.length; i++)
			path[i] = parentP[i];
		path[path.length-1] = name;
	}

	public Group(String path)
	{
		if (path.startsWith("/"))
			path = path.substring(1);
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		this.path = path.split("/");
		if (this.path.length == 1 && this.path[0].equals(""))
			this.path = new String[0];
	}

	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public boolean isChild(Group test)
	{
		String []tPath = test.getPath();
		if (tPath.length > path.length)
			return false;
		for (int i=0; i<tPath.length; i++)
			if (!tPath[i].equals(path[i]))
				return false;
		return true;
	}
	
	public boolean isTopLevel()
	{
		return path.length == 0;
	}
	
	public String[] getPath()
	{
		return path;
	}
	public String getName()
	{
		return path.length == 0 ? "/" : path[path.length - 1];
	}
	public String getParentPath()
	{
		if (path.length < 2)
			return "/";
		StringBuilder sb = new StringBuilder(path.length*10);
		for (int i=0; i<path.length-1; i++)
			sb.append("/").append(path[i]);
		return sb.toString();
	}
	
	public AttributeStatement[] getAttributeStatements()
	{
		return attributeStatements;
	}

	public void setAttributeStatements(AttributeStatement[] attributeStatements)
	{
		this.attributeStatements = attributeStatements;
	}

	public String toString()
	{
		if (path.length == 0)
			return "/";
		StringBuilder ret = new StringBuilder(path.length*10);
		for (int i=0; i<path.length; i++)
			ret.append("/").append(path[i]);
		return ret.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(path);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj== null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Group other = (Group) obj;
		if (!Arrays.equals(path, other.path))
			return false;
		return true;
	}
	
}
