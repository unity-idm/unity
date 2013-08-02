/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

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
	 * Adds a new group
	 * @param toAdd group to add
	 * @throws EngineException
	 */
	public void addGroup(Group toAdd) throws EngineException;

	/**
	 * Links the sourcePath group in targetPath group, so the sourcePath group contents becomes 
	 * contents of the targetPath. 
	 * @param targetPath
	 * @param sourcePath
	 * @throws EngineException
	 */
	public void linkGroup(String targetPath, String sourcePath) throws EngineException;

	/**
	 * Unlinks the sourcePath group from the targetPath group.
	 * @param targetPath
	 * @param sourcePath
	 * @throws EngineException
	 */
	public void unlinkGroup(String targetPath, String sourcePath) throws EngineException;
	
	/**
	 * Removes a given group. Doesn't work for '/' path.
	 * @param path
	 * @param recursive
	 * @throws EngineException
	 */
	public void removeGroup(String path, boolean recursive) throws EngineException;
	
	/**
	 * Creates a group normally. Then adds the creator to it and assigns him/her the manager role.
	 * Requires smaller permissions then addGroup.
	 * @param toAdd
	 * @throws EngineException
	 */
	public void addSelfManagedGroup(Group toAdd) throws EngineException;
	
	/**
	 * Adds a new member to the group. The entity must be a member of a parent group.
	 * @param path
	 * @param entity
	 * @param attributes an optional list of attributes to be assigned to the member in this group scope.
	 * It is especially useful in the case when group's {@link AttributesClass}es require some attributes
	 * from all members.  
	 * @throws EngineException
	 */
	public void addMemberFromParent(String path, EntityParam entity, 
			List<Attribute<?>> attributes) throws EngineException;

	/**
	 * As {@link #addMemberFromParent(String, EntityParam, List)} with an empty list of attribute classes.
	 */
	public void addMemberFromParent(String path, EntityParam entity) throws EngineException;
	
	/**
	 * Removes from the group and all subgroups if the user is in any. 
	 * Entity can not be removed from the group == '/' 
	 * @param path
	 * @param entity
	 * @throws EngineException
	 */
	public void removeMember(String path, EntityParam entity) throws EngineException;

	/**
	 * Allows to retrieve group's contents and metadata. 
	 * @param path group to be queried.
	 * @param filter what should be retrieved. Flags are defined in {@link GroupContents} class.
	 * Can be OR-ed.
	 * @return
	 * @throws EngineException
	 */
	public GroupContents getContents(String path, int filter) throws EngineException;
	
	/**
	 * Updates the group.
	 * @param path
	 * @param group new group's metadata
	 * @throws EngineException
	 */
	public void updateGroup(String path, Group group) throws EngineException;
}

