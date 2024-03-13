/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.imunity.vaadin.elements.NotEmptyComboBox;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;


class FormAttributeGroupComboBox extends NotEmptyComboBox<FormAttributeGroupComboBox.GroupModel>
{
	record GroupModel(
			String name,
			String path
	){}


	private final List<GroupModel> groups;
	private List<String> dynamicGroups;

	public FormAttributeGroupComboBox(String caption, Collection<GroupModel> groups, List<String> dynamicGroups)
	{
		super(caption);
		this.groups = new ArrayList<>(groups);
		this.dynamicGroups = new ArrayList<>(dynamicGroups);
		setItemLabelGenerator(group -> group.name);
		setRenderer(new ComponentRenderer<>(group -> new GroupItemLabel(group.name, group.path)));
		setInput(null);
	}
	
	void updateDynamicGroups(List<String> dynamicGroups)
	{
		this.dynamicGroups = new ArrayList<>(dynamicGroups);
		setInput(getValue());
	}

	public void setValue(String group)
	{
		GroupModel groupModel = groups.stream()
				.filter(model -> model.path.equals(group))
				.findAny()
				.orElse(null);
		setValue(groupModel);
	}
	
	private void setInput(GroupModel selectedValue)
	{
		groups.sort(Comparator.comparing(model -> model.name));
		List<GroupModel> processedGroups = new ArrayList<>(groups);
		for (String dynamicGroup: dynamicGroups)
		{
			String name = AttributeRegistrationParam.DYN_GROUP_PFX + dynamicGroup;
			processedGroups.add(new GroupModel(name, name));
		}
		setItems(processedGroups);
		if (!processedGroups.isEmpty())
		{
			if (selectedValue != null && processedGroups.contains(selectedValue))
				setValue(selectedValue);
			else
				setValue(processedGroups.get(0));
		}
	}
}
