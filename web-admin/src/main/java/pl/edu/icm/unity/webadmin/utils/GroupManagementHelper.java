/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.attributeclass.RequiredAttributesDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Utility class simplifies addition of an entity to a list of groups. If needed it asks for required attributes
 * with a separate dialog, for each group.
 * @author K. Benedyczak
 */
public class GroupManagementHelper
{
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private AttributesManagement attrMan; 
	private AttributeHandlerRegistry attrHandlerRegistry;
	
	public GroupManagementHelper(UnityMessageSource msg, GroupsManagement groupsMan,
			AttributesManagement attrMan, AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.attrMan = attrMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
	}

	/**
	 * Adds to groups from a deque.
	 * @param notMember
	 * @param entityId
	 * @param msg
	 * @param groupsMan
	 */
	public void addToGroup(Deque<String> notMember, long entityId, Callback callback)
	{
		EntityParam entityParam = new EntityParam(entityId);
		Collection<AttributeType> allTypes;
		try
		{
			allTypes = attrMan.getAttributeTypes();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("GroupsTree.addToGroupInitError"), e);
			return;
		}

		Map<String, AttributesClass> allACsMap;
		try
		{
			allACsMap = getAllACsMap();
		} catch (EngineException e)
		{
			return;
		}
		Deque<String> added = new ArrayDeque<>();
		addToGroupRecursive(notMember, added, allTypes, allACsMap, entityParam, callback);
	}

	private void addToGroupRecursive(final Deque<String> notMember, final Deque<String> added, 
			final Collection<AttributeType> allTypes,
			final Map<String, AttributesClass> allACsMap, final EntityParam entityParam, 
			final Callback callback)
	{
		if (notMember.isEmpty())
			return;
		final String currentGroup = notMember.pollLast();
		
		Set<String> required;
		try
		{
			required = getRequiredAttributes(allACsMap, currentGroup);
		} catch (EngineException e)
		{
			showSummary(notMember, added, currentGroup);
			return;
		}
				
		if (!required.isEmpty())
		{
			RequiredAttributesDialog attrDialog = new RequiredAttributesDialog(
					msg, msg.getMessage("GroupsTree.requiredAttributesInfo", currentGroup), 
					required, attrHandlerRegistry, allTypes, currentGroup, 
					new RequiredAttributesDialog.Callback()
					{
						@Override
						public void onConfirm(List<Attribute<?>> attributes)
						{
							addToGroupSafe(notMember, added, currentGroup, allTypes, 
									allACsMap, entityParam, attributes, callback);
						}

						@Override
						public void onCancel()
						{
							showSummary(notMember, added, currentGroup);
						}
					});
			attrDialog.show();
		} else
		{
			addToGroupSafe(notMember, added, currentGroup, allTypes, allACsMap, entityParam, 
					new ArrayList<Attribute<?>>(0), callback);
		}
	}
	
	public Map<String, AttributesClass> getAllACsMap() throws EngineException
	{
		try
		{
			return attrMan.getAttributeClasses();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("GroupsTree.addToGroupInitError"), e);
			throw e;
		}
	}
	
	public Set<String> getRequiredAttributes(Map<String, AttributesClass> allACsMap,
			String currentGroup) throws EngineException
	{
		Set<String> groupAcs;
		AttributeClassHelper acHelper;
		try
		{
			groupAcs = groupsMan.getContents(currentGroup, GroupContents.METADATA).
						getGroup().getAttributesClasses();
			acHelper = new AttributeClassHelper(allACsMap, groupAcs);
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("GroupsTree.addToGroupInitError"), e);
			throw e;
		}
		
		return acHelper.getEffectiveMandatory();
	}
	
	private void addToGroupSafe(Deque<String> notMember, Deque<String> added, 
			String currentGroup, Collection<AttributeType> allTypes,
			Map<String, AttributesClass> allACsMap, final EntityParam entityParam,
			List<Attribute<?>> attributes, Callback callback)
	{
		try
		{
			groupsMan.addMemberFromParent(currentGroup, entityParam, attributes);
			callback.onAdded(currentGroup);
			added.add(currentGroup);
			addToGroupRecursive(notMember, added, allTypes, allACsMap, entityParam, callback);
		} catch (Exception e)
		{
			showSummary(notMember, added, currentGroup);
			ErrorPopup.showError(msg.getMessage("GroupsTree.addToGroupError", 
					entityParam.getEntityId(), currentGroup), e);
		}
	}

	private void showSummary(Deque<String> notMember, Deque<String> added, String lastOne)
	{
		if (lastOne != null)
			notMember.addFirst(lastOne);
		if (notMember.isEmpty())
			return;
		if (added.isEmpty())
			ErrorPopup.showNotice(msg.getMessage("GroupsTree.addMembershipSummary"), 
					msg.getMessage("GroupsTree.notAdded", notMember));
		else
			ErrorPopup.showNotice(msg.getMessage("GroupsTree.addMembershipSummary"), 
					msg.getMessage("GroupsTree.partiallyAdded", added, notMember));
	}
	
	
	public AttributeHandlerRegistry getAttrHandlerRegistry()
	{
		return attrHandlerRegistry;
	}
	
	public AttributesManagement getAttrMan()
	{
		return attrMan;
	}
	
	public interface Callback
	{
		public void onAdded(String toGroup);
	}
}
