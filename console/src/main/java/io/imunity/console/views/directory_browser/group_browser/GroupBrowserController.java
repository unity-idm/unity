/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.group_browser;

import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.signup_and_enquiry.EnquiryFormEditor;
import io.imunity.console.views.signup_and_enquiry.RegistrationFormEditor;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
class GroupBrowserController
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, GroupBrowserController.class);

	private final MessageSource msg;
	private final GroupsManagement groupsMan;
	private final AttributeClassManagement acMan;
	private final BulkGroupQueryService bulkQueryService;
	private final RegistrationsManagement registrationMan;
	private final EnquiryManagement enquiryMan;
	private final AttributeTypeManagement attrTypeMan;
	private final ObjectFactory<RegistrationFormEditor> regFormEditorFactory;
	private final ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory;
	private final GroupDelegationConfigGenerator delConfigUtils;
	private final GroupManagementHelper groupManagementHelper;
	private final NotificationPresenter notificationPresenter;
	private final PolicyDocumentManagement policyDocumentManagement;

	GroupBrowserController(MessageSource msg, GroupsManagement groupsMan,
			AttributeClassManagement acMan, BulkGroupQueryService bulkQueryService,
			RegistrationsManagement registrationMan, EnquiryManagement enquiryMan,
			AttributeTypeManagement attrTypeMan, ObjectFactory<RegistrationFormEditor> regFormEditorFactory,
			ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory,
			GroupDelegationConfigGenerator delConfigUtils, GroupManagementHelper groupManagementHelper,
			NotificationPresenter notificationPresenter, PolicyDocumentManagement policyDocumentManagement)
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.acMan = acMan;
		this.bulkQueryService = bulkQueryService;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.attrTypeMan = attrTypeMan;
		this.regFormEditorFactory = regFormEditorFactory;
		this.enquiryFormEditorFactory = enquiryFormEditorFactory;
		this.delConfigUtils = delConfigUtils;
		this.groupManagementHelper = groupManagementHelper;
		this.notificationPresenter = notificationPresenter;
		this.policyDocumentManagement = policyDocumentManagement;
	}

	Map<String, List<Group>> getAllGroupWithSubgroups(String path)
	{
		Map<String, GroupContents> groupAndSubgroups;
		try
		{
			GroupStructuralData bulkData = bulkQueryService.getBulkStructuralData(path);
			groupAndSubgroups = bulkQueryService.getGroupAndSubgroups(bulkData);
		}
		catch (AuthorizationException e)
		{
			LOG.debug("Authorization error: ", e);
			return Map.of();
		}
		catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("GroupBrowserController.getGroupsError"), e.getMessage());
			return Map.of();
		}

		Map<String, List<Group>> groupTree = new HashMap<>();

		for (String group : groupAndSubgroups.keySet())
		{
			List<Group> subGr = new ArrayList<>();
			for (String sgr : groupAndSubgroups.get(group).getSubGroups())
			{
				subGr.add(groupAndSubgroups.get(sgr).getGroup());
			}
			groupTree.put(group, subGr);
		}
		if (!groupAndSubgroups.isEmpty() && groupAndSubgroups.get(path) != null)
		{
			groupTree.put(null, Collections.singletonList(groupAndSubgroups.get(path).getGroup()));
		}

		return groupTree;
	}

	List<TreeNode> removeGroups(Set<TreeNode> groups, boolean recursive)
	{
		List<TreeNode> removed = new ArrayList<>();
		Map<TreeNode, Throwable> errors = new HashMap<>();
		for (TreeNode groupNode : groups)
		{
			try
			{
				groupsMan.removeGroup(groupNode.getGroup().getPathEncoded(), recursive);
				removed.add(groupNode);
			} catch (Exception e)
			{
				errors.put(groupNode, e);
			}
		}
		if (removed.isEmpty())
			notificationPresenter.showError(msg.getMessage("GroupBrowserController.removeError"),
					errors.values().iterator().next().getMessage());
		else if(removed.size() != groups.size())
			notificationPresenter.showError(msg.getMessage("GroupBrowserController.removeError"),
					errors.entrySet().stream()
							.map(entry -> entry.getValue().getMessage() + ": [" + entry.getKey() + "]\n")
							.collect(Collectors.joining()) +
					msg.getMessage("GroupBrowserController.partiallyRemoved", removed));
		return removed;
	}

	void addGroup(Group group)
	{
		try
		{
			groupsMan.addGroup(group);
		} catch (EngineException e)
		{
			throw new RuntimeEngineException(e);
		}
	}

	void updateGroup(String path, Group group)
	{
		try
		{
			groupsMan.updateGroup(path, group, "manual update", "");
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupBrowserController.updateGroupError", group.getName()), e.getMessage());
		}
	}

	GroupDelegationEditConfigDialog getGroupDelegationEditConfigDialog(EventsBus bus, Group group,
			Consumer<Group> update)
	{
		Group editedGroup = getFreshGroup(group.getPathEncoded());
		return new GroupDelegationEditConfigDialog(msg, registrationMan, enquiryMan, attrTypeMan, policyDocumentManagement,
				regFormEditorFactory, enquiryFormEditorFactory, bus, delConfigUtils, editedGroup,
				delConfig ->
				{
					editedGroup.setDelegationConfiguration(delConfig);
					update.accept(editedGroup);
				}, notificationPresenter);
	}

	Group getFreshGroup(String groupPath)
	{
		GroupContents contents;
		try
		{
			contents = groupsMan.getContents(groupPath, GroupContents.METADATA);
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("GroupBrowserController.getGroupError"), e.getMessage());
			return new Group("/");
		}

		return contents.getGroup();
	}

	void bulkAddToGroup(TreeNode node, Set<EntityWithLabel> dragData)
	{
		try
		{
			groupManagementHelper.bulkAddToGroup(node.getGroup().getPathEncoded(), dragData, true);
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupBrowserController.addToGroupError", node.toString()), e.getMessage());
		}
	}

	GroupAttributesClassesDialog getGroupAttributesClassesDialog(Group group, EventsBus bus)
	{
		return new GroupAttributesClassesDialog(msg, group.getPathEncoded(), acMan, groupsMan,
				g -> bus.fireEvent(new GroupChangedEvent(group, false)), notificationPresenter);
	}
}
