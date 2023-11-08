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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.Panel;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.AttributeMetadataEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.AttributeMetadataHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;

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
	private final NotificationPresenter notificationPresenter;
	private Map<String, SingleMetadataEditor> entries;
	
	MetadataEditor(MessageSource msg, AttributeMetadataHandlerRegistry attrMetaHandlerReg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.attrMetaHandlerReg = attrMetaHandlerReg;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}
	
	private void initUI()
	{
		setMargin(false);
		setPadding(false);
		Button addNew = new Button(msg.getMessage("MetadataEditor.addButton"));
		addNew.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		
		
		addNew.addClickListener(e -> 
		{
			{
				MetadataEditDialog dialog = new MetadataEditDialog(msg, 
						new MetaUpdated()
						{
							@Override
							public void metadataUpdated(String key, String newValue)
							{
								addEntry(key, newValue);
							}
						}, 
						null, null);
				dialog.open();
			}
		});
		add(addNew);
		
		this.entries = new HashMap<String, MetadataEditor.SingleMetadataEditor>();
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
	}
	
	private class SingleMetadataEditor extends HorizontalLayout implements MetaUpdated
	{
		private String value;
		private WebAttributeMetadataHandler handler;
		
		public SingleMetadataEditor(final String key, String value, WebAttributeMetadataHandler handler)
		{
			this.handler = handler;
			this.value = value;
			setMargin(false);
			setPadding(false);
			Button edit = new Button();
			edit.setIcon(VaadinIcon.EDIT.create());
			edit.setTooltipText(msg.getMessage("MetadataEditor.editButton"));
			
			edit.addClickListener(e -> {
					MetadataEditDialog dialog = new MetadataEditDialog(msg, 
							SingleMetadataEditor.this, 
							key, 
							SingleMetadataEditor.this.value);
					dialog.open();
				});
			
			Button remove = new Button();
			remove.setIcon(VaadinIcon.DEL.create());
			remove.setTooltipText(msg.getMessage("MetadataEditor.removeButton"));
			remove.addClickListener(e -> {
					removeEntry(key);
				});
			
			Component current = handler.getRepresentation(value);
			add(edit, remove, current);
		}
		
		public String getValue()
		{
			return value;
		}
		
		@Override
		public void metadataUpdated(String key, String newValue)
		{
			this.value = newValue;
			remove(getComponentAt(2));
			Component current = handler.getRepresentation(value);
			add(current);
		}
	}
	
	private class MetadataEditDialog extends Dialog
	{
		private MetaUpdated parent;
		private AttributeMetadataEditor editor;
		private VerticalLayout editorPanel;
		private final String initialKey;
		private String key;
		private final String initialValue;
		
		public MetadataEditDialog(MessageSource msg, MetaUpdated parent, 
				String key, String initial)
		{
			setHeaderTitle(msg.getMessage("MetadataEditor.editMetadataCaption"));
			this.parent = parent;
			this.initialKey = key;
			this.initialValue = initial;
			add(getContents());
			Button closeButton = new Button(msg.getMessage("close"), e -> close());
			Button okButton = new Button(msg.getMessage("ok"), e -> onConfirm());
			okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			getFooter().add(closeButton, okButton);
		}

		protected Component getContents() 
		{
			FormLayout main = new FormLayout();
			main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
			main.addClassName("big-vaadin-form-item");
			
			editorPanel = new Panel();
			editorPanel.setMargin(false);
			if (initialKey != null)
			{
				NativeLabel info = new NativeLabel(initialKey);
				main.addFormItem(info,msg.getMessage("MetadataEditor.metaName"));
				editor = attrMetaHandlerReg.getHandler(initialKey).getEditorComponent(initialValue);
				Component editor2 = editor.getEditor();
				editorPanel.removeAll();
				editorPanel.add(editor2);
				key = initialKey;
			} else
			{
				final ComboBox<String> metaChoice = new ComboBox<>();
				Set<String> supported = attrMetaHandlerReg.getSupportedSyntaxes();
				supported.removeAll(entries.keySet());
				if (supported.isEmpty())
				{
					notificationPresenter.showNotice(msg.getMessage("notice"), 
							msg.getMessage("MetadataEditor.noMoreMetadataAvailable"));
					return new FormLayout();
				}
				metaChoice.setItems(supported);
				metaChoice.addValueChangeListener(event ->
				{
					key = metaChoice.getValue();
					editor = attrMetaHandlerReg.getHandler(key).getEditorComponent(null);
					Component editor2 = editor.getEditor();
					editorPanel.removeAll();
					editorPanel.add(editor2);
				});
				main.addFormItem(metaChoice, msg.getMessage("MetadataEditor.metaSelect"));
				metaChoice.setValue(supported.iterator().next());
			}
			main.addFormItem(editorPanel, "");
			return main;
		}
		
		private void onConfirm()
		{
			try
			{
				String value = editor.getValue();
				parent.metadataUpdated(key, value);
				close();
			} catch (FormValidationException e)
			{
				notificationPresenter.showError("", e.getMessage());
			}
		}
	}
	
	private interface MetaUpdated
	{
		public void metadataUpdated(String key, String value);
	}
}
