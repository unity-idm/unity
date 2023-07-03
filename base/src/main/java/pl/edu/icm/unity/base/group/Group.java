/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 14, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.base.group;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nDescribedObject;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.i18n.I18nStringJsonUtil;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;

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
public class Group extends I18nDescribedObject implements NamedObject, Comparable<Group>
{
	private String[] path;

	private AttributeStatement[] attributeStatements = new AttributeStatement[0];
	private Set<String> attributesClasses = new HashSet<>();
	private GroupDelegationConfiguration delegationConfiguration;
	private boolean publicGroup = false;
	private Map<String, GroupProperty> properties = new HashMap<>();

	private String encodedPath;
	
	
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
		encodedPath = encodePath();
	}

	public Group(String path)
	{
		setPath(path);
		displayedName = new I18nString(toString());
		description = new I18nString();
		delegationConfiguration = new GroupDelegationConfiguration(false);
		publicGroup = false;
	}
	
	public List<String> getPathsChain()
	{
		List<String> paths = new ArrayList<>();	
		paths.add(getPathEncoded());
		if (isTopLevel())
			return paths;
		Group grp = clone();
		do
		{
			grp = new Group(grp.getParentPath());
			paths.add(grp.getPathEncoded());
		} while (!grp.isTopLevel());
		return paths;	
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
		target.setProperties(properties.values());
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
	
	public static boolean isDirectChild(String group, String potentialParent)
	{
		return isChild(group, potentialParent, false) && !group.substring(potentialParent.length() + 1).contains("/");
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
	
	public static Set<Group> getRootsOfSet(Set<Group> source)
	{
		Set<Group> onlyParents = new HashSet<>(source);

		for (Group g1 : source)
		{
			for (Group g2 : source)
			{
				if (g2.isChildNotSame(g1))
				{
					onlyParents.remove(g2);
				}
			}
		}
		return onlyParents;
	}
	
	public static Set<Group> getOnlyChildrenOfSet(Set<Group> source)
	{
		Set<Group> onlyChildren = new HashSet<>(source);

		for (Group g1 : source)
		{
			for (Group g2 : source)
			{
				if (g1.isChildNotSame(g2))
				{
					onlyChildren.remove(g2);
				}
			}
		}
		return onlyChildren;
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
	
	public boolean isChildNotSame(Group test)
	{
		return isChild(toString(), test.toString(), false);
	}

	public boolean isTopLevel()
	{
		return path.length == 0;
	}

	public String[] getPath()
	{
		return path;
	}

	public String getPathEncoded()
	{
		return encodedPath;
	}

	@Override
	public String getName()
	{
		return toString();
	}

	/**
	 * This is likely a go-to method to present group name to a person.
	 * If displayed name was set to non default value (which is sadly group path :/) then it is returned.
	 * Otherwise last component of the path is returned. 
	 */
	public I18nString getDisplayedNameShort(MessageSource msg)
	{
		I18nString displayedName = getDisplayedName();
		return toString().equals(displayedName.getValue(msg)) ? new I18nString(getNameShort()) : displayedName;
	}
	
	public void setPath(String path)
	{
		if (path.equals("/"))
		{
			this.path = new String[0];
			this.encodedPath = encodePath();
			return;
		}
		if (path.startsWith("/"))
			path = path.substring(1);
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		this.path = path.split("/");
		if (this.path.length == 1 && this.path[0].equals(""))
			this.path = new String[0];
		this.encodedPath = encodePath();
	}

	private String encodePath()
	{
		if (path.length == 0)
			return "/";
		StringBuilder ret = new StringBuilder(path.length * 10);
		for (int i = 0; i < path.length; i++)
			ret.append("/").append(path[i]);
		return ret.toString();		
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
	
	public Map<String, GroupProperty> getProperties()
	{
		return properties;
	}

	public void setProperties(Collection<GroupProperty> properties)
	{
		this.properties = properties.stream().collect(Collectors.toMap(p -> p.key, p -> p));
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
		return getPathEncoded();
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
		main.set("properties",  Constants.MAPPER.valueToTree(properties.values()));
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
			try
			{
				GroupDelegationConfiguration delegationConfig = Constants.MAPPER.treeToValue(
						main.get("delegationConfiguration"), GroupDelegationConfiguration.class);
				setDelegationConfiguration(delegationConfig);
			} catch (Exception e)
			{
				throw new InternalException(
						"Can't deserialize group delegation configuration from JSON", e);
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
		
		if (JsonUtil.notNull(main, "properties"))
		{
			ArrayNode attrsNode = (ArrayNode) main.get("properties");
			attrsNode.forEach(n -> {
				ObjectNode attrNode = (ObjectNode) n;
				GroupProperty readP = Constants.MAPPER.convertValue(attrNode, 
						 GroupProperty.class);
				properties.put(readP.key, readP);				
			});
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
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + Arrays.hashCode(path);
		return result;
	}
	
	@Override
	public int compareTo(Group toCompare)
	{
		return toString().compareTo(toCompare.toString());	
		
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
		if (properties == null)
		{
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
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
