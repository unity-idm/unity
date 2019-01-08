/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.attributeclass.RequiredAttributesDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Utility class simplifies addition of an entity to a list of groups. If needed it asks for required attributes
 * with a separate dialog, for each group.
 * @author K. Benedyczak
 */
@Component
public class GroupManagementHelper
{
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private AttributeTypeManagement attrMan; 
	private AttributeHandlerRegistry attrHandlerRegistry;
	private AttributeClassManagement acMan;
	private EntityManagement identitiesMan;
	
	@Autowired
	public GroupManagementHelper(UnityMessageSource msg, GroupsManagement groupsMan,
			AttributeTypeManagement attrMan, 
			AttributeClassManagement acMan,
			AttributeHandlerRegistry attrHandlerRegistry,
			EntityManagement identitiesMan)
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.attrMan = attrMan;
		this.acMan = acMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.identitiesMan = identitiesMan;
	}

	/**
	 * @return set with required attributes in a group from its ACs
	 */
	public Set<String> getRequiredAttributes(String group) throws EngineException
	{
		Map<String, AttributesClass> allACs = getAllACsMap();
		return getRequiredAttributes(allACs, group);
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
			NotificationPopup.showError(msg, msg.getMessage("GroupsTree.addToGroupInitError"), e);
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

	
	public void bulkAddToGroup(String finalGroup, Collection<EntityWithLabel> entities, boolean withConfirm)
	{
		Map<EntityWithLabel, Deque<String>> toAdd = new HashMap<>();

		for (EntityWithLabel en : entities)
		{
			Deque<String> groups = getMissingEntityGroups(finalGroup, en);
			if (groups == null)
				return;
			toAdd.put(en, groups);
		}

		int missingSize = 0;
		for (Deque<String> notMember : toAdd.values())
			missingSize += notMember.size();
		if (missingSize == 0)
		{
			String info = msg.getMessage("GroupsTree.alreadyMember", 
					MessageUtils.createConfirmFromStrings(msg, entities), finalGroup);
			NotificationPopup.showNotice(info, "");
			return;
		}

		if (withConfirm)
		{
			String confirmationMessage = msg.getMessage("GroupsTree.confirmAddToGroup",
				MessageUtils.createConfirmFromStrings(msg, entities), finalGroup); 
			ConfirmDialog confirm = new ConfirmDialog(msg, confirmationMessage, () -> doAddToGroup(toAdd));
			confirm.show();
		} else
		{
			doAddToGroup(toAdd);
		}
	}

	private void doAddToGroup(Map<EntityWithLabel, Deque<String>> toAdd)
	{
		for (Map.Entry<EntityWithLabel, Deque<String>> entry : toAdd.entrySet())
		{
			addToGroup(
					entry.getValue(),
					entry.getKey().getEntity().getId(),
					s -> {});
		}
	}
	
	
	private Deque<String> getMissingEntityGroups(String finalGroup, EntityWithLabel entity)
	{
		EntityParam entityParam = new EntityParam(entity.getEntity().getId());
		Collection<String> existingGroups;
		try
		{
			existingGroups = identitiesMan.getGroups(entityParam).keySet();
		} catch (EngineException e1)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupsTree.getMembershipError", entity),
					e1);
			return null;
		}
		final Deque<String> notMember = Group.getMissingGroups(finalGroup, existingGroups);
		
		return notMember;
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
						public void onConfirm(List<Attribute> attributes)
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
					new ArrayList<Attribute>(0), callback);
		}
	}

	private Map<String, AttributesClass> getAllACsMap() throws EngineException
	{
		try
		{
			return acMan.getAttributeClasses();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("GroupsTree.addToGroupInitError"), e);
			throw e;
		}
	}
	
	private Set<String> getRequiredAttributes(Map<String, AttributesClass> allACsMap,
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
			NotificationPopup.showError(msg, msg.getMessage("GroupsTree.addToGroupInitError"), e);
			throw e;
		}
		
		return acHelper.getEffectiveMandatory();
	}
	
	private void addToGroupSafe(Deque<String> notMember, Deque<String> added, 
			String currentGroup, Collection<AttributeType> allTypes,
			Map<String, AttributesClass> allACsMap, final EntityParam entityParam,
			List<Attribute> attributes, Callback callback)
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
			NotificationPopup.showError(msg, msg.getMessage("GroupsTree.addToGroupError", 
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
			NotificationPopup.showNotice(msg.getMessage("GroupsTree.addMembershipSummary"), 
					msg.getMessage("GroupsTree.notAdded", notMember));
		else
			NotificationPopup.showSuccess(msg.getMessage("GroupsTree.addMembershipSummary"), 
					msg.getMessage("GroupsTree.partiallyAdded", added, notMember));
	}
	
	
	public AttributeHandlerRegistry getAttrHandlerRegistry()
	{
		return attrHandlerRegistry;
	}
	
	public interface Callback
	{
		public void onAdded(String toGroup);
	}
}
