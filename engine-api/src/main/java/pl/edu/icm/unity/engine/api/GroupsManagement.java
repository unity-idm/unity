/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.group.GroupsChain;


/**
 * Internal engine API for groups management.
 */
public interface GroupsManagement
{
	/**
	 * @return true if the given group exists
	 */
	boolean isPresent(String group) throws AuthorizationException;
	
	/**
	 * Adds a new group
	 * @param toAdd group to add
	 */
	void addGroup(Group toAdd) throws EngineException;

	/**
	 * Adds a new group in a recursive way
	 * @param toAdd group to add
	 * @param withParents flag to add group recursively
	 */
	void addGroup(Group toAdd, boolean withParents) throws EngineException;

	/**
	 * Adds new groups
	 * @param requestedGroups set of groups to add
	 */
	void addGroups(Set<Group> toAdd) throws EngineException;
	
	/**
	 * Removes a given group. Doesn't work for '/' path.
	 */
	void removeGroup(String path, boolean recursive) throws EngineException;
	
	/**
	 * Adds a new member to the group. The entity must be a member of a parent group. This method should be used
	 * when adding to a group in effect of remote account mapping. 
	 * @param attributes an optional list of attributes to be assigned to the member in this group scope.
	 * It is especially useful in the case when group's {@link AttributesClass}es require some attributes
	 * from all members.  
	 * @param idp Id of Idp responsible (typically implicitly via translation profile) for addition to the group
	 * @param translationProfile name of an input translation profile which created the membership 
	 */
	void addMemberFromParent(String path, EntityParam entity, 
			List<Attribute> attributes, String idp, String translationProfile) throws EngineException;
	
	/**
	 * Adds a new member to the group. The entity must be a member of a parent group.
	 * This method must be used when adding to a group manually.
	 * @param attributes an optional list of attributes to be assigned to the member in this group scope.
	 * It is especially useful in the case when group's {@link AttributesClass}es require some attributes
	 * from all members.  
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
	 */
	void removeMember(String path, EntityParam entity) throws EngineException;

	/**
	 * Allows to retrieve group's contents and metadata. 
	 * @param path group to be queried.
	 * @param filter what should be retrieved. Flags are defined in {@link GroupContents} class.
	 * Can be OR-ed.
	 */
	GroupContents getContents(String path, int filter) throws EngineException;
	
	/**
	 * Retrieves list of all groups matching a given ant-style wildcard
	 */
	List<Group> getGroupsByWildcard(String pathWildcard);
	
	/**
	 * @return all groups which are children of the root group (including grand children). The root group
	 * is also in the returned set. 
	 */
	Set<String> getChildGroups(String root) throws EngineException;
	
	/**
	 * Updates the group, without specifying any special audit log information 
	 */
	void updateGroup(String path, Group group) throws EngineException;

	/**
	 * Updates the group and pass information: changed property and new value used for audit log only
	 */
	void updateGroup(String path, Group group, String changedProperty, String newValue) throws EngineException;
	
	
	/**
	 * @return GroupChain for given group
	 */
	GroupsChain getGroupsChain(String path) throws EngineException;

	Map<String, Group> getAllGroups() throws EngineException;

}




