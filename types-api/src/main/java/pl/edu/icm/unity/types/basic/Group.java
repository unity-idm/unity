/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 14, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.types.basic;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nDescribedObject;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Group holds set of other elements: other groups and identities. This class
 * only denotes group, it doesn't hold group's content.
 * <p>
 * Each group can have a list of {@link AttributeStatement}s assigned. Group
 * member can automatically get attributes from a statement if she fulfills the
 * statement's condition.
 * <p>
 * Each group can have a set of {@link AttributesClass}es assigned. Members of
 * the group have those classes automatically assigned.
 * 
 * @author K. Benedyczak
 */
public class Group extends I18nDescribedObject implements NamedObject
{
	private String[] path;

	private AttributeStatement[] attributeStatements = new AttributeStatement[0];
	private Set<String> attributesClasses = new HashSet<String>();
	private GroupDelegationConfiguration delegationConfiguration;
	private boolean publicGroup = false;

	public Group(Group parent, String name)
	{
		if (name == null || name.equals("") || name.contains("/"))
			throw new IllegalArgumentException(
					"Group name must be a non empty string without '/' character");
		String parentP[] = parent.getPath();
		path = new String[parentP.length + 1];
		for (int i = 0; i < parentP.length; i++)
			path[i] = parentP[i];
		path[path.length - 1] = name;
		displayedName = new I18nString(toString());
		description = new I18nString();
		delegationConfiguration = new GroupDelegationConfiguration(false);
		publicGroup = false;
	}

	public Group(String path)
	{
		setPath(path);
		displayedName = new I18nString(toString());
		description = new I18nString();
		delegationConfiguration = new GroupDelegationConfiguration(false);
		publicGroup = false;
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
		target.setDelegationConfiguration(delegationConfiguration);
		target.setPublic(publicGroup);
		return target;
	}

	/**
	 * @param group
	 * @param potentialParent
	 * @return true only if potentialParent is group's parent and is not
	 *         equal to group
	 */
	public static boolean isChild(String group, String potentialParent)
	{
		return isChild(group, potentialParent, false);
	}

	/**
	 * @param group
	 * @param potentialParent
	 * @return true only if potentialParent is group's parent or is not
	 *         equal to group
	 */
	public static boolean isChildOrSame(String group, String potentialParent)
	{
		return isChild(group, potentialParent, true);
	}

	private static boolean isChild(String group, String potentialParent, boolean allowSame)
	{
		if (allowSame && group.equals(potentialParent))
			return true;
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

	/**
	 * Computes deque of full group names which are not in the collection of
	 * existingGroups and are on the path to the finalGroup (inclusive).
	 * 
	 * @param finalGroup
	 * @param existingGroups
	 * @return
	 */
	public static Deque<String> getMissingGroups(String finalGroup,
			Collection<String> existingGroups)
	{
		Group group = new Group(finalGroup);
		String[] path = group.getPath();
		final Deque<String> notMember = new ArrayDeque<>(path.length);
		for (int i = path.length - 1; i >= 0
				&& !existingGroups.contains(group.toString()); i--)
		{
			notMember.addLast(group.toString());
			if (!group.isTopLevel())
				group = new Group(group.getParentPath());
		}
		return notMember;
	}

	/**
	 * Changes part of this group path, when parent group is renamed.
	 * 
	 * @param originalParentName
	 * @param newParentName
	 * @return updated path
	 */
	public static String renameParent(String group, String originalParentName,
			String newParentName)
	{
		return newParentName + group.substring(originalParentName.length());
	}

	public boolean isChild(Group test)
	{
		String[] tPath = test.getPath();
		if (tPath.length > path.length)
			return false;
		for (int i = 0; i < tPath.length; i++)
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
		StringBuilder sb = new StringBuilder(path.length * 10);
		for (int i = 0; i < path.length - 1; i++)
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

	public GroupDelegationConfiguration getDelegationConfiguration()
	{
		return delegationConfiguration;
	}

	public void setDelegationConfiguration(GroupDelegationConfiguration delegationConfiguration)
	{
		this.delegationConfiguration = delegationConfiguration;
	}

	public boolean isPublic()
	{
		return publicGroup;
	}

	public void setPublic(boolean publicGroup)
	{
		this.publicGroup = publicGroup;
	}

	/**
	 * @return last component of the group path
	 */
	public String getNameShort()
	{
		return path.length == 0 ? "/" : path[path.length - 1];
	}

	@Override
	public String toString()
	{
		if (path.length == 0)
			return "/";
		StringBuilder ret = new StringBuilder(path.length * 10);
		for (int i = 0; i < path.length; i++)
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
		for (AttributeStatement as : getAttributeStatements())
			ases.add(as.toJson());
		ArrayNode aces = main.putArray("attributesClasses");
		for (String ac : getAttributesClasses())
			aces.add(ac);

		
		GroupDelegationConfiguration delegationConfig = getDelegationConfiguration();
		if (delegationConfig == null)
		{
			delegationConfig = new GroupDelegationConfiguration(false);
		}
		main.set("delegationConfiguration",  Constants.MAPPER.valueToTree(delegationConfig));
		main.put("publicGroup", isPublic());

		return main;
	}

	public void fromJsonBase(ObjectNode main)
	{
		setDescription(I18nStringJsonUtil.fromJson(main.get("i18nDescription"),
				main.get("description")));
		I18nString displayedName = I18nStringJsonUtil.fromJson(main.get("displayedName"));
		if (displayedName.getDefaultValue() == null)
			displayedName.setDefaultValue(toString());
		setDisplayedName(displayedName);

		ArrayNode jsonStatements = (ArrayNode) main.get("attributeStatements");
		int asLen = jsonStatements.size();
		AttributeStatement[] statements = new AttributeStatement[asLen];
		int i = 0;
		for (JsonNode n : jsonStatements)
			statements[i++] = new AttributeStatement((ObjectNode) n);
		setAttributeStatements(statements);

		ArrayNode jsonAcs = (ArrayNode) main.get("attributesClasses");
		Set<String> acs = new HashSet<>();
		for (JsonNode e : jsonAcs)
			acs.add(e.asText());
		setAttributesClasses(acs);

		if (JsonUtil.notNull(main, "delegationConfiguration"))
		{
			ObjectMapper jsonMapper = Constants.MAPPER;
			String v;
			try
			{
				v = jsonMapper.writeValueAsString(
						main.get("delegationConfiguration"));
				GroupDelegationConfiguration config = jsonMapper.readValue(v,
						GroupDelegationConfiguration.class);
				setDelegationConfiguration(config);
			} catch (Exception e)
			{
				throw new InternalException(
						"Can't deserialize group delegation configuration from JSON",
						e);
			}

		} else
		{
			setDelegationConfiguration(new GroupDelegationConfiguration(false));
		}

		if (JsonUtil.notNull(main, "publicGroup"))
		{
			setPublic(main.get("publicGroup").asBoolean());
		} else
		{
			setPublic(false);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(attributeStatements);
		result = prime * result
				+ ((attributesClasses == null) ? 0 : attributesClasses.hashCode());
		result = prime * result + Arrays.hashCode(path);
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
		Group other = (Group) obj;
		if (!Arrays.equals(attributeStatements, other.attributeStatements))
			return false;
		if (attributesClasses == null)
		{
			if (other.attributesClasses != null)
				return false;
		} else if (!attributesClasses.equals(other.attributesClasses))
			return false;
		if (delegationConfiguration == null)
		{
			if (other.delegationConfiguration != null)
				return false;
		} else if (!delegationConfiguration.equals(other.delegationConfiguration))
			return false;
		if (publicGroup != other.publicGroup)
			return false;
		if (!Arrays.equals(path, other.path))
			return false;
		return true;
	}
}
