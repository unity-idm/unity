/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;

import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.endpoint.common.forms.groups.GroupMultiComboBox;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor of a preset {@link GroupSelection}.
 * 
 * @author Krzysztof Benedyczak
 */
public class PresetMembershipEditor extends PresetEditorBase<GroupSelection>
{
	private final List<GroupRegistrationParam> formParams;
	private GroupMultiComboBox selection;
	private final List<Group> allGroups;
	private FormLayout wrapper;
	private GroupRegistrationParam groupRegistrationParam;
	
	public PresetMembershipEditor(MessageSource msg, List<Group> allGroups, List<GroupRegistrationParam> formParams)
	{
		super(msg);
		this.allGroups = allGroups;
		this.formParams = formParams;
	}

	@Override
	protected Optional<GroupSelection> getValueInternal() throws FormValidationException
	{
		return Optional.of(new GroupSelection(selection.getSelectedGroupsWithoutParents()));
	}
	
	@Override
	public void setEditedComponentPosition(int position)
	{
		wrapper.removeAll();
		groupRegistrationParam = formParams.get(position);
		selection.setMultiSelect(groupRegistrationParam.isMultiSelect());
		List<Group> items = GroupPatternMatcher.filterByIncludeGroupsMode(GroupPatternMatcher.filterMatching(allGroups, 
				groupRegistrationParam.getGroupPath()), groupRegistrationParam.getIncludeGroupsMode());
		selection.setItems(items);
		wrapper.addFormItem(selection, groupRegistrationParam.getGroupPath());
		super.setEditedComponentPosition(position);
	}
	
	@Override
	protected Component getEditorComponentsInternal(PrefilledEntry<GroupSelection> value,
			int position)
	{
		wrapper = new FormLayout();
		wrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		GroupRegistrationParam groupRegistrationParam = formParams.get(position);
		selection = new GroupMultiComboBox(msg);
		selection.setMultiSelect(groupRegistrationParam.isMultiSelect());
		selection.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		setEditedComponentPosition(position);
		return wrapper;
	}
	
	@Override
	protected String getTitle()
	{
		return msg.getMessage("PresetEditor.activeGroup", groupRegistrationParam.getGroupPath());
	}
}
