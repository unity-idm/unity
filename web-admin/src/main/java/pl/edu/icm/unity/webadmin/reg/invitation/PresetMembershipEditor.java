/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.List;
import java.util.Optional;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.groups.GroupsSelection;

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
	
	public PresetMembershipEditor(UnityMessageSource msg, List<Group> allGroups, List<GroupRegistrationParam> formParams)
	{
		super(msg);
		this.allGroups = allGroups;
		this.formParams = formParams;
	}

	@Override
	protected Optional<GroupSelection> getValueInternal() throws FormValidationException
	{
		return Optional.of(new GroupSelection(selection.getSelectedGroups()));
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
		selection = GroupsSelection.getGroupsSelection(msg, true, false);
		setEditedComponentPosition(position);
		return selection;
	}
}
