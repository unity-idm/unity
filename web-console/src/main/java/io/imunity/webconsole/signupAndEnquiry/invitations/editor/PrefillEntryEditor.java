/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.groups.GroupsSelection;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistryV8;

@PrototypeComponent
class PrefillEntryEditor extends TabSheet
{
	private ListOfEmbeddedElements<PrefilledEntry<IdentityParam>> presetIdentities;
	private ListOfEmbeddedElements<GroupSelectionPair> presetGroups;
	private ListOfEmbeddedElements<PrefilledEntry<Attribute>> presetAttributes;

	private final List<Group> allGroups;
	private final MessageSource msg;
	private final IdentityEditorRegistryV8 identityEditorRegistry;
	private final AttributeHandlerRegistryV8 attrHandlersRegistry;
	private final Map<String, AttributeType> attrTypes;

	@Autowired
	PrefillEntryEditor(MessageSource msg, IdentityEditorRegistryV8 identityEditorRegistry,
	                   AttributeHandlerRegistryV8 attrHandlersRegistry, AttributeTypeManagement attributeTypeManagement,
	                   GroupsManagement groupsManagement) throws EngineException

	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.attrTypes = attributeTypeManagement.getAttributeTypesAsMap();
		this.allGroups = groupsManagement.getGroupsByWildcard("/**");
	}

	public void setInput(BaseForm input)
	{
		removeAllComponents();
		if (input == null)
		{
			return;
		}

		List<IdentityRegistrationParam> idParams = input.getIdentityParams();
		int idParamsNum = idParams == null ? 0 : idParams.size();
		presetIdentities = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetIdentityEditor(identityEditorRegistry, idParams, msg);
		}, idParamsNum, idParamsNum, true);
		presetIdentities.setCaption(msg.getMessage("InvitationEditor.identities"));
		if (idParamsNum > 0)
			addTabWithMargins(presetIdentities);

		List<AttributeRegistrationParam> attrParams = input.getAttributeParams();
		int attrParamsNum = attrParams == null ? 0 : attrParams.size();
		presetAttributes = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetAttributeEditor(msg, attrParams, attrHandlersRegistry, attrTypes);
		}, attrParamsNum, attrParamsNum, true);
		presetAttributes.setCaption(msg.getMessage("InvitationEditor.attributes"));
		if (attrParamsNum > 0)
			addTabWithMargins(presetAttributes);

		List<GroupRegistrationParam> groupParams = input.getGroupParams();
		int groupParamsNum = groupParams == null ? 0 : groupParams.size();
		presetGroups = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetMembershipEditorWithAllowedGroups(msg, allGroups, groupParams);
		}, groupParamsNum, groupParamsNum, true);
		presetGroups.setCaption(msg.getMessage("InvitationEditor.groups"));
		if (groupParamsNum > 0)
			addTabWithMargins(presetGroups);
	}

	private void addTabWithMargins(Component src)
	{
		VerticalLayout wrapper = new VerticalLayout(src);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		addTab(wrapper).setCaption(src.getCaption());
		src.setCaption("");
	}

	public void prefill(FormPrefill toSet) throws FormValidationException
	{
		prefill(presetIdentities.getElements(), toSet.getIdentities());
		prefill(presetAttributes.getElements(), toSet.getAttributes());
		prefill(presetGroups.getElements().stream().map(v -> v.groupSelection).collect(Collectors.toList()),
				toSet.getGroupSelections());
		prefill(presetGroups.getElements().stream().map(v -> v.allowedGroupSelection).collect(Collectors.toList()),
				toSet.getAllowedGroups());
	}

	private <T> void prefill(List<T> input, Map<Integer, T> output)
	{
		for (int i = 0; i < input.size(); i++)
		{
			T element = input.get(i);
			if (element != null)
				output.put(i, element);
		}
	}

	private static class PresetMembershipEditorWithAllowedGroups implements Editor<GroupSelectionPair>
	{

		private PresetMembershipEditor memberEditor;
		private GroupsSelection allowedGroupSelection;
		private List<Group> allGroups;
		private List<GroupRegistrationParam> formParams;

		public PresetMembershipEditorWithAllowedGroups(MessageSource msg, List<Group> allGroups,
				List<GroupRegistrationParam> formParams)
		{
			this.formParams = formParams;
			this.allGroups = allGroups;
			memberEditor = new PresetMembershipEditor(msg, allGroups, formParams);
			allowedGroupSelection = GroupsSelection.getGroupsSelection(msg, true, false);
			allowedGroupSelection.setCaption(msg.getMessage("InvitationEditor.limitTo"));
			allowedGroupSelection.setDescription(msg.getMessage("InvitationEditor.limitToDescription"));
		}

		@Override
		public void setEditedComponentPosition(int position)
		{
			memberEditor.setEditedComponentPosition(position);
			GroupRegistrationParam groupRegistrationParam = formParams.get(position);
			List<Group> items = GroupPatternMatcher.filterByIncludeGroupsMode(
					GroupPatternMatcher.filterMatching(allGroups, groupRegistrationParam.getGroupPath()),
					groupRegistrationParam.getIncludeGroupsMode());
			allowedGroupSelection.setItems(items);
		}

		@Override
		public ComponentsContainer getEditorComponent(GroupSelectionPair value, int position)
		{
			ComponentsContainer container = new ComponentsContainer();
			container.add(allowedGroupSelection);
			container.add(memberEditor.getEditorComponent(value != null ? value.groupSelection : null, position)
					.getComponents());
			return container;
		}

		@Override
		public GroupSelectionPair getValue() throws FormValidationException
		{
			return new GroupSelectionPair(memberEditor.getValue(),
					new GroupSelection(allowedGroupSelection.getSelectedGroupsWithParents()));
		}
	}

	private static class GroupSelectionPair
	{
		public final PrefilledEntry<GroupSelection> groupSelection;
		public final GroupSelection allowedGroupSelection;

		public GroupSelectionPair(PrefilledEntry<GroupSelection> groupSelection, GroupSelection allowedGroupSelection)
		{
			this.groupSelection = groupSelection;
			this.allowedGroupSelection = allowedGroupSelection;
		}
	}

	
	@org.springframework.stereotype.Component
	public static class PrefillEntryEditorFactory
	{
		private ObjectFactory<PrefillEntryEditor> editorFactory;

		public PrefillEntryEditorFactory(ObjectFactory<PrefillEntryEditor> editor)
		{
			this.editorFactory = editor;
		}

		public PrefillEntryEditor getEditor() throws EngineException
		{
			return editorFactory.getObject();
		}
	}
}
