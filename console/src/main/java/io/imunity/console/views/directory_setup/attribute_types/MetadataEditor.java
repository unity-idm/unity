/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.attribute_types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.AttributeMetadataHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Editing of multiple metadata entries.
 * 
 * 
 * @author P.Piernik
 */
class MetadataEditor extends VerticalLayout
{
	private final MessageSource msg;
	private final AttributeMetadataHandlerRegistry attrMetaHandlerReg;
	private Map<String, SingleMetadataEditor> entries;
	private ComboBox<String> metaChoice;
	private Button addNew; 
	MetadataEditor(MessageSource msg, AttributeMetadataHandlerRegistry attrMetaHandlerReg)
	{
		this.msg = msg;
		this.attrMetaHandlerReg = attrMetaHandlerReg;
		initUI();
	}
	
	private void initUI()
	{
		setMargin(false);
		setPadding(false);
		setSpacing(false);
		addNew = new Button();
		addNew.addThemeName(ButtonVariant.LUMO_ICON.name());
		addNew.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		metaChoice = new ComboBox<>();
		metaChoice.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		metaChoice.setOverlayClassName(CssClassNames.HIDDEN_COMBO_CHECKMARK.getName());
		
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setPadding(false);
		wrapper.setMargin(false);
		wrapper.add(metaChoice, addNew);		
		add(wrapper);
		addNew.addClickListener(e -> {
			addEntry(metaChoice.getValue(), "");		
		});
		
		this.entries = new HashMap<String, MetadataEditor.SingleMetadataEditor>();
		refreshCombo();
	}
	
	void refreshCombo()
	{
		Set<String> supported = attrMetaHandlerReg.getSupportedSyntaxes();
		supported.removeAll(entries.keySet());
		metaChoice.setItems(supported);
		metaChoice.setEnabled(!supported.isEmpty());
		addNew.setEnabled(!supported.isEmpty());
		if (!supported.isEmpty())
		{
			metaChoice.setValue(supported.iterator().next());
		}
	}
	
	
	void setInput(Map<String, String> initial)
	{
		Iterator<String> entriesIt = entries.keySet().iterator();
		while(entriesIt.hasNext())
		{
			String toDel = entriesIt.next();
			remove(entries.get(toDel));
			entriesIt.remove();
		}
		for (Map.Entry<String, String> metaE: initial.entrySet())
			addEntry(metaE.getKey(), metaE.getValue());
		
		refreshCombo();
	}
	
	Map<String, String> getValue()
	{
		Map<String, String> ret = new HashMap<>(entries.size());
		for (Map.Entry<String, SingleMetadataEditor> entry: entries.entrySet())
			ret.put(entry.getKey(), entry.getValue().getValue());
		return ret;
	}
	
	private void removeEntry(String key)
	{
		SingleMetadataEditor editor = entries.remove(key);
		remove(editor);
	}
	
	private void addEntry(String key, String newValue)
	{
		WebAttributeMetadataHandler handler = attrMetaHandlerReg.getHandler(key);
		SingleMetadataEditor editor = new SingleMetadataEditor(key, newValue, handler);
		entries.put(key, editor);
		add(editor);
		refreshCombo();
	}
	
	private class SingleMetadataEditor extends HorizontalLayout 
	{
		private final String value;
		
		public SingleMetadataEditor(final String key, String value, WebAttributeMetadataHandler handler)
		{
			this.value = value;
			setMargin(false);
			setPadding(false);
			
			Button remove = new Button();
			remove.setIcon(VaadinIcon.TRASH.create());
			remove.setTooltipText(msg.getMessage("MetadataEditor.removeButton"));
			remove.addClickListener(e -> {
					removeEntry(key);
					refreshCombo();
				});
			Component current = handler.getRepresentation(value);
			add(remove, current);
			setAlignItems(Alignment.CENTER);
		}
		
		public String getValue()
		{
			return value;
		}
	}
}
