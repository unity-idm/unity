/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;

import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.endpoint.common.forms.groups.GroupMultiComboBox;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ListOfEmbeddedElements;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ListOfEmbeddedElementsStub.Editor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
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
import pl.edu.icm.unity.webui.common.FormValidationException;

@PrototypeComponent
class PrefillEntryEditor extends TabSheet
{
	private ListOfEmbeddedElements<PrefilledEntry<IdentityParam>> presetIdentities;
	private ListOfEmbeddedElements<GroupSelectionPair> presetGroups;
	private ListOfEmbeddedElements<PrefilledEntry<Attribute>> presetAttributes;

	private final List<Group> allGroups;
	private final MessageSource msg;
	private final IdentityEditorRegistry identityEditorRegistry;
	private final AttributeHandlerRegistry attrHandlersRegistry;
	private final Map<String, AttributeType> attrTypes;

	@Autowired
	PrefillEntryEditor(MessageSource msg, IdentityEditorRegistry identityEditorRegistry,
	                   AttributeHandlerRegistry attrHandlersRegistry, AttributeTypeManagement attributeTypeManagement,
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
		if (idParamsNum > 0)
			addTabWithMargins(presetIdentities, msg.getMessage("InvitationEditor.identities"));

		List<AttributeRegistrationParam> attrParams = input.getAttributeParams();
		int attrParamsNum = attrParams == null ? 0 : attrParams.size();
		presetAttributes = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetAttributeEditor(msg, attrParams, attrHandlersRegistry, attrTypes);
		}, attrParamsNum, attrParamsNum, true);
		if (attrParamsNum > 0)
			addTabWithMargins(presetAttributes, msg.getMessage("InvitationEditor.attributes"));

		List<GroupRegistrationParam> groupParams = input.getGroupParams();
		int groupParamsNum = groupParams == null ? 0 : groupParams.size();
		presetGroups = new ListOfEmbeddedElements<>(msg, () ->
		{
			return new PresetMembershipEditorWithAllowedGroups(msg, allGroups, groupParams);
		}, groupParamsNum, groupParamsNum, true);
		if (groupParamsNum > 0)
			addTabWithMargins(presetGroups, msg.getMessage("InvitationEditor.groups"));
	}

	private void addTabWithMargins(Component src, String caption)
	{
		VerticalLayout wrapper = new VerticalLayout(src);
		wrapper.setWidthFull();
		wrapper.setSpacing(false);
		wrapper.setPadding(false);
		add(caption, wrapper);
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

	private  class PresetMembershipEditorWithAllowedGroups implements Editor<GroupSelectionPair>
	{

		private PresetMembershipEditor memberEditor;
		private GroupMultiComboBox allowedGroupSelection;
		private List<Group> allGroups;
		private List<GroupRegistrationParam> formParams;
		private FormLayout wrapper;
		public PresetMembershipEditorWithAllowedGroups(MessageSource msg, List<Group> allGroups,
				List<GroupRegistrationParam> formParams)
		{
			this.formParams = formParams;
			this.allGroups = allGroups;
			memberEditor = new PresetMembershipEditor(msg, allGroups, formParams);
			allowedGroupSelection = new GroupMultiComboBox(msg);
			allowedGroupSelection.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
			allowedGroupSelection.setTooltipText(msg.getMessage("InvitationEditor.limitToDescription"));
			wrapper = new FormLayout();
			wrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
			wrapper.addFormItem(allowedGroupSelection, msg.getMessage("InvitationEditor.limitTo"));
			
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
			container.add(wrapper);
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
