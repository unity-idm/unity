/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import io.imunity.vaadin.elements.NotEmptyComboBox;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static pl.edu.icm.unity.base.registration.AttributeRegistrationParam.DYN_GROUP_PFX;


public class FormAttributeGroupComboBox extends NotEmptyComboBox<String>
{
	private final MessageSource msg;
	private final List<String> groups;
	private List<String> dynamicGroups;

	public FormAttributeGroupComboBox(String caption, MessageSource msg, Collection<String> groups, 
			List<String> dynamicGroups)
	{
		super(caption);
		this.msg = msg;
		this.groups = new ArrayList<>(groups);
		this.dynamicGroups = new ArrayList<>(dynamicGroups);
		setItemLabelGenerator(this::getCaption);
		setInput(null);
	}
	
	void updateDynamicGroups(List<String> dynamicGroups)
	{
		this.dynamicGroups = new ArrayList<>(dynamicGroups);
		setInput(getValue());
	}
	
	private String getCaption(String item)
	{
		return item.startsWith(DYN_GROUP_PFX) ? 
				msg.getMessage("RegistrationFormEditor.dynamicGroup", 
						item.substring(DYN_GROUP_PFX.length())) : 
				item;
	}
	
	private void setInput(String selectedValue)
	{
		Collections.sort(groups);
		List<String> processedGroups = new ArrayList<>(groups);
		for (String dynamicGroup: dynamicGroups)
			processedGroups.add(AttributeRegistrationParam.DYN_GROUP_PFX + dynamicGroup);
		setItems(processedGroups);
		if (!processedGroups.isEmpty())
		{
			if (selectedValue != null && processedGroups.contains(selectedValue))
				setValue(selectedValue);
			else
				setValue(processedGroups.get(0));
		}
	}

	@Override
	public String getValue()
	{
		return super.getValue();
	}
}
