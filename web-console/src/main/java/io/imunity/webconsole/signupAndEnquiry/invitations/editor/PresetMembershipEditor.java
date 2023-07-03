/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import java.util.List;
import java.util.Optional;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.groups.GroupsSelection;
import pl.edu.icm.unity.webui.common.groups.OptionalGroupsSelection;

/**
 * Editor of a preset {@link GroupSelection}.
 * 
 * @author Krzysztof Benedyczak
 */
public class PresetMembershipEditor extends PresetEditorBase<GroupSelection>
{
	private List<GroupRegistrationParam> formParams;
	private GroupsSelection selection;
	private List<Group> allGroups;
	
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
		GroupRegistrationParam groupRegistrationParam = formParams.get(position);
		selection.setCaption(groupRegistrationParam.getGroupPath());
		selection.setMultiSelectable(groupRegistrationParam.isMultiSelect());
		List<Group> items = GroupPatternMatcher.filterByIncludeGroupsMode(GroupPatternMatcher.filterMatching(allGroups, 
				groupRegistrationParam.getGroupPath()), groupRegistrationParam.getIncludeGroupsMode());
		selection.setItems(items);
	}
	
	@Override
	protected Component getEditorComponentsInternal(PrefilledEntry<GroupSelection> value,
			int position)
	{
		GroupRegistrationParam groupRegistrationParam = formParams.get(position);
		selection = new OptionalGroupsSelection(msg, groupRegistrationParam.isMultiSelect());
		setEditedComponentPosition(position);
		return selection;
	}
}
