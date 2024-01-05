/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.directory_setup.attribute_classes.RequiredAttributesDialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
public class GroupManagementHelper
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, GroupManagementHelper.class);

	private final MessageSource msg;
	private final GroupsManagement groupsMan;
	private final AttributeTypeManagement attrMan;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final AttributeClassManagement acMan;
	private final EntityManagement identitiesMan;
	private final NotificationPresenter notificationPresenter;

	GroupManagementHelper(MessageSource msg, GroupsManagement groupsMan,
			AttributeTypeManagement attrMan, 
			AttributeClassManagement acMan,
			AttributeHandlerRegistry attrHandlerRegistry,
			EntityManagement identitiesMan,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.attrMan = attrMan;
		this.acMan = acMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.identitiesMan = identitiesMan;
		this.notificationPresenter = notificationPresenter;
	}

	public Set<String> getRequiredAttributes(String group) throws EngineException
	{
		Map<String, AttributesClass> allACs = getAllACsMap();
		return getRequiredAttributes(allACs, group);
	}
	

	public void addToGroup(Deque<String> notMember, long entityId, Callback callback)
	{
		EntityParam entityParam = new EntityParam(entityId);
		Collection<AttributeType> allTypes;
		try
		{
			allTypes = attrMan.getAttributeTypes();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("GroupsTree.addToGroupInitError"), e.getMessage());
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
			toAdd.put(en, groups);
		}

		int missingSize = 0;
		for (Deque<String> notMember : toAdd.values())
			missingSize += notMember.size();
		if (missingSize == 0)
		{
			String info = msg.getMessage("GroupsTree.alreadyMember", 
					MessageUtils.createConfirmFromStrings(msg, entities), finalGroup);
			notificationPresenter.showNotice(info, "");
			return;
		}

		if (withConfirm)
		{
			String confirmationMessage = msg.getMessage("GroupsTree.confirmAddToGroup",
				MessageUtils.createConfirmFromStrings(msg, entities), finalGroup); 
			ConfirmDialog confirm = new ConfirmDialog();
			confirm.setConfirmButton(msg.getMessage("ok"), e -> doAddToGroup(toAdd));
			confirm.setCancelable(true);
			confirm.add(new Span(confirmationMessage));
			confirm.open();
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
			notificationPresenter.showError(
					msg.getMessage("GroupsTree.getMembershipError", entity),
					e1.getMessage());
			return new LinkedList<>();
		}

		return Group.getMissingGroups(finalGroup, existingGroups);
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
			LOG.error(e);
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
					}, notificationPresenter);
			attrDialog.open();
		} else
		{
			addToGroupSafe(notMember, added, currentGroup, allTypes, allACsMap, entityParam,
					new ArrayList<>(0), callback);
		}
	}

	private Map<String, AttributesClass> getAllACsMap() throws EngineException
	{
		try
		{
			return acMan.getAttributeClasses();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("GroupsTree.addToGroupInitError"), e.getMessage());
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
			notificationPresenter.showError(msg.getMessage("GroupsTree.addToGroupInitError"), e.getMessage());
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
			notificationPresenter.showError(msg.getMessage("GroupsTree.addToGroupError",
					entityParam.getEntityId(), currentGroup), e.getMessage());
		}
	}

	private void showSummary(Deque<String> notMember, Deque<String> added, String lastOne)
	{
		if (lastOne != null)
			notMember.addFirst(lastOne);
		if (notMember.isEmpty())
			return;
		if (added.isEmpty())
			notificationPresenter.showNotice(msg.getMessage("GroupsTree.addMembershipSummary"),
					msg.getMessage("GroupsTree.notAdded", notMember));
		else
			notificationPresenter.showSuccess(msg.getMessage("GroupsTree.addMembershipSummary"),
					msg.getMessage("GroupsTree.partiallyAdded", added, notMember));
	}
	
	
	public AttributeHandlerRegistry getAttrHandlerRegistry()
	{
		return attrHandlerRegistry;
	}
	
	public interface Callback
	{
		void onAdded(String toGroup);
	}
}
