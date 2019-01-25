/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;

/**
 * Internal engine API for delegated groups management
 * @author P.Piernik
 *
 */
public interface DelegatedGroupManagement
{
	/**
	 * Adds group
	 * 
	 * @param projectPath project group path
	 * @param parentPath parent group path
	 * @param groupName new group name
	 * @param isPublic group access mode
	 * @throws EngineException
	 */
	void addGroup(String projectPath, String parentPath, I18nString groupName, boolean isPublic)
			throws EngineException;

	/**
	 * Removes group
	 * 
	 * @param projectPath project group path
	 * @param path removed group path
	 * @throws EngineException
	 */
	void removeGroup(String projectPath, String path) throws EngineException;
	
	/**
	 * Allows to retrieve group's contents and metadata. 
	 * 
	 * @param path group to be queried.
	 * @return
	 * @throws EngineException
	 */
	DelegatedGroupContents getContents(String projectPath, String path) throws EngineException;

	/**
	 * Sets group display name
	 * 
	 * @param projectPath project group path
	 * @param path renamed group path
	 * @param newName 
	 * @throws EngineException
	 */
	void setGroupDisplayedName(String projectPath, String path, I18nString newName)
			throws EngineException;

	/**
	 * Updates group access mode
	 * 
	 * @param projectPath project group path
	 * @param path updated group path
	 * @param isPublic indicates is group public or private mode
	 * @throws EngineException
	 */
	void setGroupAccessMode(String projectPath, String path, boolean isPublic)
			throws EngineException;

	/**
	 * 
	 * @param projectPath project group path
	 * @param groupPath group to be queried
	 * @return keys of the returned map include the selected group and all its children. Values are 
	 * objects with group's metadata and subgroups
	 * @throws EngineException
	 */

	Map<String, DelegatedGroupContents> getGroupAndSubgroups(String projectPath, String groupPath)
			throws EngineException;

	
	/**
	 * Gets attribute displayed name
	 * 
	 * @param projectPath project group path
	 * @param attributeName
	 * @return attribute display name
	 * @throws EngineException
	 */
	String getAttributeDisplayedName(String projectPath, String attributeName)
			throws EngineException;

	/**
	 * Update value of group authorization role attribute 
	 * @param projectPath project group path
	 * @param entityId attribute owner
	 * @param role value to set
	 * @throws EngineException
	 */
	void setGroupAuthorizationRole(String projectPath, long entityId, GroupAuthorizationRole role)
			throws EngineException;

	/**
	 * @param entityId project manager
	 * @return All project group of entity
	 * @throws EngineException
	 */
	List<DelegatedGroup> getProjectsForEntity(long entityId) throws EngineException;

	/**
	 * Adds a new member to the group
	 * 
	 * @param projectPath project group path
	 * @param groupPath
	 * @param entityId entity id to add
	 * @throws EngineException
	 */
	void addMemberToGroup(String projectPath, String groupPath, long entityId)
			throws EngineException;

	/**
	 * Removes from the group and all subgroups if the user is in any. 
	 * Entity can not be removed from the group == '/' 
	 * 
	 * @param projectPath project group path
	 * @param groupPath group removing from 
	 * @param entityId entity id to remove
	 * @throws EngineException
	 */
	void removeMemberFromGroup(String projectPath, String groupPath, long entityId)
			throws EngineException;

	/**
	 * Gets delegated group members
	 * @param projectPath project group path
	 * @param groupPath
	 * @return
	 * @throws EngineException
	 */
	List<DelegatedGroupMember> getDelegatedGroupMemebers(String projectPath, String groupPath)
			throws EngineException;

}
