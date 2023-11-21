/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser.groupbrowser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.signupAndEnquiry.forms.EnquiryFormEditorV8;
import io.imunity.webconsole.signupAndEnquiry.forms.RegistrationFormEditorV8;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component("GroupBrowserControllerV8")
class GroupBrowserController
{
	private MessageSource msg;

	private GroupsManagement groupsMan;
	private EntityManagement identitiesMan;
	private AttributeClassManagement acMan;
	private BulkGroupQueryService bulkQueryService;
	private RegistrationsManagement registrationMan;
	private PolicyDocumentManagement policyDocumentManagement;
	private EnquiryManagement enquiryMan;
	private AttributeTypeManagement attrTypeMan;
	private ObjectFactory<RegistrationFormEditorV8> regFormEditorFactory;
	private ObjectFactory<EnquiryFormEditorV8> enquiryFormEditorFactory;
	private GroupDelegationConfigGenerator delConfigUtils;
	private GroupManagementHelper groupManagementHelper;

	@Autowired
	GroupBrowserController(MessageSource msg, GroupsManagement groupsMan, EntityManagement identitiesMan,
			AttributeClassManagement acMan, BulkGroupQueryService bulkQueryService,
			RegistrationsManagement registrationMan, PolicyDocumentManagement policyDocumentManagement, 
			EnquiryManagement enquiryMan,
			AttributeTypeManagement attrTypeMan, ObjectFactory<RegistrationFormEditorV8> regFormEditorFactory,
			ObjectFactory<EnquiryFormEditorV8> enquiryFormEditorFactory,
			GroupDelegationConfigGenerator delConfigUtils, GroupManagementHelper groupManagementHelper)
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.identitiesMan = identitiesMan;
		this.acMan = acMan;
		this.bulkQueryService = bulkQueryService;
		this.registrationMan = registrationMan;
		this.policyDocumentManagement = policyDocumentManagement;
		this.enquiryMan = enquiryMan;
		this.attrTypeMan = attrTypeMan;
		this.regFormEditorFactory = regFormEditorFactory;
		this.enquiryFormEditorFactory = enquiryFormEditorFactory;
		this.delConfigUtils = delConfigUtils;
		this.groupManagementHelper = groupManagementHelper;
	}

	Map<String, List<Group>> getAllGroupWithSubgroups(String path) throws ControllerException
	{
		Map<String, GroupContents> groupAndSubgroups;
		try
		{
			GroupStructuralData bulkData = bulkQueryService.getBulkStructuralData(path);
			groupAndSubgroups = bulkQueryService.getGroupAndSubgroups(bulkData);		
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("GroupBrowserController.getGroupsError"), e);
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
			groupTree.put(null, Arrays.asList(groupAndSubgroups.get(path).getGroup()));
		}

		return groupTree;
	}

	Set<String> getEntitiesGroup(long entityId) throws ControllerException
	{
		try
		{
			return identitiesMan.getGroups(new EntityParam(entityId)).keySet();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("GroupBrowserController.getEntitiesGroupsError"),
					e);
		}
	}

	void removeGroups(Set<TreeNode> groups, boolean recursive) throws ControllerException
	{

		List<String> removed = new ArrayList<>();
		try
		{
			for (TreeNode groupNode : groups)
			{
				groupsMan.removeGroup(groupNode.getGroup().getPathEncoded(), recursive);
				removed.add(groupNode.toString());
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				throw new ControllerException(msg.getMessage("GroupBrowserController.removeError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("GroupBrowserController.removeError"),
						msg.getMessage("GroupBrowserController.partiallyRemoved", removed), e);
			}
		}

	}

	void addGroup(Group group) throws ControllerException
	{
		try
		{
			groupsMan.addGroup(group);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupBrowserController.addGroupError", group.getName()), e);
		}
	}

	void updateGroup(String path, Group group) throws ControllerException
	{
		try
		{
			groupsMan.updateGroup(path, group, "manual update", "");
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupBrowserController.updateGroupError", group.getName()), e);
		}
	}

	GroupStructuralData getBulkStructuralData(String path) throws ControllerException
	{
		try
		{
			return bulkQueryService.getBulkStructuralData(path);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("GroupBrowserController.getGroupError"), e);
		}

	}

	Map<String, GroupContents> getGroupAndSubgroups(GroupStructuralData bulkData) throws ControllerException
	{
		try
		{
			return bulkQueryService.getGroupAndSubgroups(bulkData);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("GroupBrowserController.getGroupError"), e);
		}
	}

	GroupDelegationEditConfigDialog getGroupDelegationEditConfigDialog(EventsBus bus, Group group,
			Consumer<Group> update) throws ControllerException
	{
		Group editedGroup = getFreshGroup(group.getPathEncoded()); 
		return new GroupDelegationEditConfigDialog(msg, registrationMan, enquiryMan, attrTypeMan, policyDocumentManagement,
				regFormEditorFactory, enquiryFormEditorFactory, bus, delConfigUtils, editedGroup,
				delConfig -> {
					editedGroup.setDelegationConfiguration(delConfig);
					update.accept(editedGroup);
				});
	}

	Group getFreshGroup(String groupPath) throws ControllerException
	{
		GroupContents contents;
		try
		{
			contents = groupsMan.getContents(groupPath, GroupContents.METADATA);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("GroupBrowserController.getGroupError"), e);
		}
		
		Group editedGroup = contents.getGroup();
		return editedGroup;
	}

	void bulkAddToGroup(TreeNode node, Set<EntityWithLabel> dragData) throws ControllerException
	{
		try
		{
			groupManagementHelper.bulkAddToGroup(node.getGroup().getPathEncoded(), dragData, true);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupBrowserController.addToGroupError", node.toString()), e);
		}
	}

	GroupAttributesClassesDialog getGroupAttributesClassesDialog(Group group, EventsBus bus)
	{
		return new GroupAttributesClassesDialog(msg, group.getPathEncoded(), acMan, groupsMan,
				g -> bus.fireEvent(new GroupChangedEvent(group)));
	}
}
