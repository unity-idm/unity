/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import io.imunity.console.components.NonEmptyComboBox;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.*;

class RemoveAttributeColumnDialog extends ConfirmDialog
{
	private static final String GROUP_PREFIX = "@/";
	private final Callback callback;
	private final Set<String> alreadyUsedInRoot;
	private final Set<String> alreadyUsedInCurrent;
	private final String currentGroup;
	private final MessageSource msg;
	private Map<String, String> labelsToAttr;
	private ComboBox<String> attributeType;

	RemoveAttributeColumnDialog(MessageSource msg, Set<String> alreadyUsedInRoot, 
			Set<String> alreadyUsedInCurrent, String currentGroup, Callback callback)
	{
		this.msg = msg;
		this.alreadyUsedInCurrent = alreadyUsedInCurrent;
		this.alreadyUsedInRoot = alreadyUsedInRoot;
		this.callback = callback;
		this.currentGroup = currentGroup;
		setHeader(msg.getMessage("RemoveAttributeColumnDialog.caption"));
		setWidth("40em");
		setHeight("15em");
		setCancelable(true);
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		add(getContents());
	}

	private FormLayout getContents()
	{
		labelsToAttr = new HashMap<>();
		attributeType = new NonEmptyComboBox<>(msg.getMessage("RemoveAttributeColumnDialog.info"));
		attributeType.setWidthFull();
		List<String> values = new ArrayList<>();
		for (String at: alreadyUsedInRoot)
		{
			String value = toRootGroupLabel(at);
			values.add(value);
			labelsToAttr.put(value, at + GROUP_PREFIX );
		}
		for (String at: alreadyUsedInCurrent)
		{
			String value = toCurrentGroupLabel(at);
			values.add(value);
			labelsToAttr.put(value, at + GROUP_PREFIX + currentGroup );
		}
		attributeType.setItems(values);
		if (!alreadyUsedInRoot.isEmpty())
			attributeType.setValue(toRootGroupLabel(alreadyUsedInRoot.iterator().next()));
		else if (!alreadyUsedInCurrent.isEmpty())
			attributeType.setValue(toCurrentGroupLabel(alreadyUsedInCurrent.iterator().next()));

		FormLayout main = new FormLayout();
		main.addFormItem(attributeType, msg.getMessage("RemoveAttributeColumnDialog.attribute"));
		main.setSizeFull();
		return main;
	}

	private String toCurrentGroupLabel(String attribute)
	{
		return attribute + "@" + currentGroup + " (current)";
	}

	private String toRootGroupLabel(String attribute)
	{
		return attribute + GROUP_PREFIX + " (fixed)";
	}

	
	private void onConfirm()
	{
		String selected = attributeType.getValue();
		if (selected == null)
		{
			close();
			return;
		}
		String parsable = labelsToAttr.get(selected);
		int split = parsable.lastIndexOf(GROUP_PREFIX);
		String group = parsable.substring(split+2);
		String attr = parsable.substring(0, split);
		callback.onChosen(attr, group);
		close();
	}
	
	interface Callback 
	{
		void onChosen(String attributeType, String group);
	}
}
