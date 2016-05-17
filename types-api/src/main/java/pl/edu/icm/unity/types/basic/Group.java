/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 14, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.types.basic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.I18nDescribedObject;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Group holds set of other elements: other groups and identities. This class only denotes group, 
 * it doesn't hold group's content.
 * <p>
 * Each group can have a list of {@link AttributeStatement}s assigned. Group member can automatically get attributes
 * from a statement if she fulfills the statement's condition.
 * <p>
 * Each group can have a set of {@link AttributesClass}es assigned. Members of the group have those classes 
 * automatically assigned.
 * 
 * @author K. Benedyczak
 */
public class Group extends I18nDescribedObject implements NamedObject
{
	private String[] path;
	
	private AttributeStatement[] attributeStatements = new AttributeStatement[0];
	private Set<String> attributesClasses = new HashSet<String>();
	private boolean displayedNameSet = false;

	public Group(Group parent, String name)
	{
		if (name == null || name.equals("") || name.contains("/"))
			throw new IllegalArgumentException("Group name must be a non empty string without '/' character");
		String parentP[] = parent.getPath();
		path = new String[parentP.length + 1];
		for (int i=0; i<parentP.length; i++)
			path[i] = parentP[i];
		path[path.length-1] = name;
		displayedName = new I18nString(toString());
		description = new I18nString();
	}

	public Group(String path)
	{
		setPath(path);
		displayedName = new I18nString(toString());
		description = new I18nString();
	}

	@JsonCreator
	public Group(ObjectNode src)
	{
		fromJson(src);
	}

	@Override
	public Group clone()
	{
		Group target = new Group(toString());
		target.setDescription(description.clone());
		target.setDisplayedName(displayedName.clone());
		Set<String> acClone = new HashSet<>(attributesClasses);
		target.setAttributesClasses(acClone);
		target.setAttributeStatements(attributeStatements.clone());
		return target;
	}
	
	public static boolean isChild(String group, String potentialParent)
	{
		int gLen = group.length();
		int pLen = potentialParent.length();
		if (gLen <= pLen)
			return false;
		if (pLen == 1 && potentialParent.charAt(0) == '/')
			return true;
		if (group.charAt(pLen) != '/')
			return false;
		if (!group.startsWith(potentialParent))
			return false;
		return true;
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
	
	@Override
	public String getName()
	{
		return toString();
	}
	
	public void setPath(String path)
	{
		if (path.equals("/"))
		{
			this.path = new String[0];
			return;
		}
		if (path.startsWith("/"))
			path = path.substring(1);
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		this.path = path.split("/");
		if (this.path.length == 1 && this.path[0].equals(""))
			this.path = new String[0];
	}
	
	public String getRelativeName()
	{
		return path.length == 0 ? "/" : path[path.length - 1];
	}

	public String getParentPath()
	{
		if (path.length == 0)
			return null;
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

	public Set<String> getAttributesClasses()
	{
		return attributesClasses;
	}

	public void setAttributesClasses(Set<String> attributesClasses)
	{
		this.attributesClasses = attributesClasses;
	}

	/**
	 * @return if displayed name was set to something different then the default value (i.e. the value returned 
	 * by {@link #toString()}) it is returned. Otherwise the {@link #getName()} result is returned. 
	 */
	public I18nString getDisplayedNameShort()
	{
		return displayedNameSet ? displayedName : new I18nString(getName());
	}
	
	@Override
	public void setDisplayedName(I18nString displayedName)
	{
		displayedNameSet = !toString().equals(displayedName.getDefaultValue()) ||
				!displayedName.getMap().isEmpty();
		super.setDisplayedName(displayedName);
	}
	
	@Override
	public String toString()
	{
		if (path.length == 0)
			return "/";
		StringBuilder ret = new StringBuilder(path.length*10);
		for (int i=0; i<path.length; i++)
			ret.append("/").append(path[i]);
		return ret.toString();
	}

	private void fromJson(ObjectNode main)
	{
		setPath(main.get("path").asText());
		fromJsonBase(main);
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = toJsonBase();
		main.put("path", getName());
		return main;
	}
	
	public ObjectNode toJsonBase()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.set("i18nDescription", I18nStringJsonUtil.toJson(getDescription()));
		main.set("displayedName", I18nStringJsonUtil.toJson(getDisplayedName()));
		ArrayNode ases = main.putArray("attributeStatements");
		for (AttributeStatement as: getAttributeStatements())
			ases.add(as.toJson());
		ArrayNode aces = main.putArray("attributesClasses");
		for (String ac: getAttributesClasses())
			aces.add(ac);
		return main;
	}

	public void fromJsonBase(ObjectNode main)
	{
		setDescription(I18nStringJsonUtil.fromJson(main.get("i18nDescription"),
				main.get("description")));
		setDisplayedName(main.has("displayedName") ? 
				I18nStringJsonUtil.fromJson(main.get("displayedName")) : 
					new I18nString(toString()));
		
		ArrayNode jsonStatements = (ArrayNode) main.get("attributeStatements");
		int asLen = jsonStatements.size();
		AttributeStatement[] statements = new AttributeStatement[asLen];
		int i=0;
		for (JsonNode n: jsonStatements)
			statements[i++] = new AttributeStatement((ObjectNode) n);
		setAttributeStatements(statements);

		ArrayNode jsonAcs = (ArrayNode) main.get("attributesClasses");
		Set<String> acs = new HashSet<>();
		for (JsonNode e: jsonAcs)
			acs.add(e.asText());
		setAttributesClasses(acs);
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
