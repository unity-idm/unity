/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;


/**
 * Internal engine API for groups management.
 * 
 * @author K. Benedyczak
 */
public interface GroupsManagement
{
	/**
	 * @param group
	 * @return true if the given group exists
	 * @throws AuthorizationException 
	 */
	boolean isPresent(String group) throws AuthorizationException;
	
	/**
	 * Adds a new group
	 * @param toAdd group to add
	 * @throws EngineException
	 */
	void addGroup(Group toAdd) throws EngineException;

	/**
	 * Removes a given group. Doesn't work for '/' path.
	 * @param path
	 * @param recursive
	 * @throws EngineException
	 */
	void removeGroup(String path, boolean recursive) throws EngineException;
	
	/**
	 * Adds a new member to the group. The entity must be a member of a parent group. This method should be used
	 * when adding to a group in effect of remote account mapping. 
	 * @param path
	 * @param entity
	 * @param attributes an optional list of attributes to be assigned to the member in this group scope.
	 * It is especially useful in the case when group's {@link AttributesClass}es require some attributes
	 * from all members.  
	 * @param idp Id of Idp responsible (typically implicitly via translation profile) for addition to the group
	 * @param translationProfile name of an input translation profile which created the membership 
	 * @throws EngineException
	 */
	void addMemberFromParent(String path, EntityParam entity, 
			List<Attribute> attributes, String idp, String translationProfile) throws EngineException;
	
	/**
	 * Adds a new member to the group. The entity must be a member of a parent group.
	 * This method must be used when adding to a group manually.
	 * @param path
	 * @param entity
	 * @param attributes an optional list of attributes to be assigned to the member in this group scope.
	 * It is especially useful in the case when group's {@link AttributesClass}es require some attributes
	 * from all members.  
	 * @throws EngineException
	 */
	void addMemberFromParent(String path, EntityParam entity, 
			List<Attribute> attributes) throws EngineException;

	/**
	 * As {@link #addMemberFromParent(String, EntityParam, List)} with an empty list of attribute classes.
	 */
	void addMemberFromParent(String path, EntityParam entity) throws EngineException;
	
	/**
	 * Removes from the group and all subgroups if the user is in any. 
	 * Entity can not be removed from the group == '/' 
	 * @param path
	 * @param entity
	 * @throws EngineException
	 */
	void removeMember(String path, EntityParam entity) throws EngineException;

	/**
	 * Allows to retrieve group's contents and metadata. 
	 * @param path group to be queried.
	 * @param filter what should be retrieved. Flags are defined in {@link GroupContents} class.
	 * Can be OR-ed.
	 * @return
	 * @throws EngineException
	 */
	GroupContents getContents(String path, int filter) throws EngineException;
	
	/**
	 * Retrieves list of all groups matching a given ant-style wildcard
	 */
	List<Group> getGroupsByWildcard(String pathWildcard);
	
	/**
	 * @param root
	 * @return all groups which are children of the root group (including grand children). The root group
	 * is also in the returned set. 
	 * @throws EngineException
	 */
	Set<String> getChildGroups(String root) throws EngineException;
	
	/**
	 * Updates the group.
	 * @param path
	 * @param group new group's metadata
	 * @throws EngineException
	 */
	void updateGroup(String path, Group group) throws EngineException;
}

