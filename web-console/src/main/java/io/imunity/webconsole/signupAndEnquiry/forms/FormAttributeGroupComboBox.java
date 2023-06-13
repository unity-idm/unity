/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms;

import static pl.edu.icm.unity.base.registration.AttributeRegistrationParam.DYN_GROUP_PFX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;


/**
 * Combo box allowing to choose either a fixed group or a dynamic one basing on existing group selectors in a form.
 *  
 * @author K. Benedyczak
 */
public class FormAttributeGroupComboBox extends ComboBox<String>
{
	private List<String> groups;
	private List<String> dynamicGroups;
	private MessageSource msg;

	public FormAttributeGroupComboBox(String caption, MessageSource msg, Collection<String> groups, 
			List<String> dynamicGroups)
	{
		super(caption);
		this.msg = msg;
		this.groups = new ArrayList<>(groups);
		this.dynamicGroups = new ArrayList<>(dynamicGroups);
		setItemCaptionGenerator(this::getCaption);
		setEmptySelectionAllowed(false);
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
			{
				setSelectedItem(selectedValue);
			} else
			{
				setSelectedItem(processedGroups.get(0));
			}
		}
	}

	@Override
	public String getValue()
	{
		return super.getValue();
	}
}
