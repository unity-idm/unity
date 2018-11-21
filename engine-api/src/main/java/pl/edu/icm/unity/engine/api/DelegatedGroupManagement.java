/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api;

import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.DelegatedGroupMember;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupAuthorizationRole;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * 
 * @author P.Piernik
 *
 */
public interface DelegatedGroupManagement
{
	/**
	 * {@link GroupsManagement#addGroup(Group)}
	 */
	void addGroup(String projectPath, String parentPath, I18nString groupName, boolean isOpen)
			throws EngineException;

	/**
	 * {@link GroupsManagement#removeGroup(Group)}
	 */
	void removeGroup(String projectPath, String path, boolean recursive) throws EngineException;

	/**
	 * Set group display name
	 * 
	 * @param path
	 * @param newName
	 * @throws EngineException
	 */
	void setGroupDisplayedName(String projectPath, String path, I18nString newName)
			throws EngineException;

	/**
	 * Set group access mode
	 * 
	 * @param isOpen
	 */
	void setGroupAccessMode(String projectPath, String path, boolean isOpen)
			throws EngineException;

	/**
	 * Get group members
	 * @param projectPath
	 * @param groupPath
	 * @return
	 * @throws EngineException
	 */

	List<DelegatedGroupMember> getGroupMembers(String projectPath, String groupPath)
			throws EngineException;

	/**
	 * @return keys of the returned map include the selected group and all
	 *         its children. Values are objects with group's metadata and
	 *         subgroups (but without members)
	 */
	Map<String, GroupContents> getGroupAndSubgroups(String projectPath, String groupPath)
			throws EngineException;

	/**
	 * 
	 * @param groupPath
	 * @return map
	 * @throws EngineException
	 */
	Map<String, String> getAdditionalAttributeNamesForProject(String projectPath)
			throws EngineException;

	/**
	 * 
	 * @param path
	 * @param entityId
	 * @param role
	 * @throws EngineException
	 */
	void setGroupAuthorizationRole(String path, long entityId, GroupAuthorizationRole role)
			throws EngineException;

	/**
	 * 
	 * @param entityId
	 * @return
	 * @throws EngineException
	 */
	List<Group> getProjectsForEntity(long entityId) throws EngineException;

	/**
	 * 
	 * @param projectPath
	 * @param groupPath
	 * @param entityId
	 * @throws EngineException
	 */
	void addMemberToGroup(String projectPath, String groupPath, long entityId)
			throws EngineException;

	/**
	 * 
	 * @param projectPath
	 * @param groupPath
	 * @param entityId
	 * @throws EngineException
	 */
	void removeMemberFromGroup(String projectPath, String groupPath, long entityId)
			throws EngineException;

	
}
