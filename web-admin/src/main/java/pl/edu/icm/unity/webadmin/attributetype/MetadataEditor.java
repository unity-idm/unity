/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataEditor;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.webui.common.attrmetadata.WebAttributeMetadataHandler;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Editing of multiple metadata entries.
 * 
 * 
 * @author K. Benedyczak
 */
public class MetadataEditor extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributeMetadataHandlerRegistry attrMetaHandlerReg;
	private Map<String, SingleMetadataEditor> entries;

	public MetadataEditor(UnityMessageSource msg, AttributeMetadataHandlerRegistry attrMetaHandlerReg)
	{
		this.msg = msg;
		this.attrMetaHandlerReg = attrMetaHandlerReg;
		initUI();
	}
	
	private void initUI()
	{
		setMargin(false);
		Button addNew = new Button(msg.getMessage("MetadataEditor.addButton"));
		addNew.setIcon(Images.add.getResource());
		
		addNew.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
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
				dialog.show();
			}
		});
		addComponent(addNew);
		
		this.entries = new HashMap<String, MetadataEditor.SingleMetadataEditor>();
	}
	
	public void setInput(Map<String, String> initial)
	{
		Iterator<String> entriesIt = entries.keySet().iterator();
		while(entriesIt.hasNext())
		{
			String toDel = entriesIt.next();
			removeComponent(entries.get(toDel));
			entriesIt.remove();
		}
		for (Map.Entry<String, String> metaE: initial.entrySet())
			addEntry(metaE.getKey(), metaE.getValue());
	}
	
	public Map<String, String> getValue()
	{
		Map<String, String> ret = new HashMap<>(entries.size());
		for (Map.Entry<String, SingleMetadataEditor> entry: entries.entrySet())
			ret.put(entry.getKey(), entry.getValue().getValue());
		return ret;
	}
	
	private void removeEntry(String key)
	{
		SingleMetadataEditor editor = entries.remove(key);
		removeComponent(editor);
	}
	
	private void addEntry(String key, String newValue)
	{
		WebAttributeMetadataHandler handler = attrMetaHandlerReg.getHandler(key);
		SingleMetadataEditor editor = new SingleMetadataEditor(key, newValue, handler);
		entries.put(key, editor);
		addComponent(editor);
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
			Button edit = new Button();
			edit.setDescription(msg.getMessage("MetadataEditor.editButton"));
			edit.setIcon(Images.edit.getResource());
			edit.setStyleName(Styles.vButtonSmall.toString());
			edit.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					MetadataEditDialog dialog = new MetadataEditDialog(msg, 
							SingleMetadataEditor.this, 
							key, 
							SingleMetadataEditor.this.value);
					dialog.show();
				}
			});
			
			Button remove = new Button();
			remove.setDescription(msg.getMessage("MetadataEditor.removeButton"));
			remove.setIcon(Images.delete.getResource());
			remove.setStyleName(Styles.vButtonSmall.toString());
			remove.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					removeEntry(key);
				}
			});
			
			Component current = handler.getRepresentation(value);
			current.setWidth(100, Unit.PERCENTAGE);
			addComponents(edit, remove, current);
		}
		
		public String getValue()
		{
			return value;
		}
		
		@Override
		public void metadataUpdated(String key, String newValue)
		{
			this.value = newValue;
			removeComponent(getComponent(2));
			Component current = handler.getRepresentation(value);
			addComponent(current);
		}
	}
	
	
	private class MetadataEditDialog extends AbstractDialog
	{
		private MetaUpdated parent;
		private AttributeMetadataEditor editor;
		private SafePanel editorPanel;
		private final String initialKey;
		private String key;
		private final String initialValue;
		
		public MetadataEditDialog(UnityMessageSource msg, MetaUpdated parent, 
				String key, String initial)
		{
			super(msg, msg.getMessage("MetadataEditor.editMetadataCaption"));
			this.parent = parent;
			this.initialKey = key;
			this.initialValue = initial;
		}

		@Override
		protected Component getContents() throws Exception
		{
			FormLayout main = new CompactFormLayout();
			editorPanel = new SafePanel();
			editorPanel.setWidth(100, Unit.PERCENTAGE);
			editorPanel.setStyleName(Styles.vPanelLight.toString());
			if (initialKey != null)
			{
				Label info = new Label(initialKey);
				info.setCaption(msg.getMessage("MetadataEditor.metaName"));
				main.addComponent(info);
				editor = attrMetaHandlerReg.getHandler(initialKey).getEditorComponent(initialValue);
				Component editor2 = editor.getEditor();
				editor2.setWidth(100, Unit.PERCENTAGE);
				editorPanel.setContent(editor2);
				key = initialKey;
			} else
			{
				final ComboBox<String> metaChoice = new ComboBox<>(
						msg.getMessage("MetadataEditor.metaSelect"));
				metaChoice.setEmptySelectionAllowed(false);
				Set<String> supported = attrMetaHandlerReg.getSupportedSyntaxes();
				supported.removeAll(entries.keySet());
				if (supported.isEmpty())
				{
					NotificationPopup.showNotice(msg.getMessage("notice"), 
							msg.getMessage("MetadataEditor.noMoreMetadataAvailable"));
					throw new FormValidationException();
				}
				metaChoice.setItems(supported);
				metaChoice.addValueChangeListener(event ->
				{
					key = metaChoice.getValue();
					editor = attrMetaHandlerReg.getHandler(key).getEditorComponent(null);
					Component editor2 = editor.getEditor();
					editor2.setWidth(100, Unit.PERCENTAGE);
					editorPanel.setContent(editor2);
				});
				main.addComponent(metaChoice);
				metaChoice.setSelectedItem(supported.iterator().next());
			}
			main.addComponent(editorPanel);
			return main;
		}

		
		
		@Override
		protected void onConfirm()
		{
			try
			{
				String value = editor.getValue();
				parent.metadataUpdated(key, value);
				close();
			} catch (FormValidationException e)
			{
				NotificationPopup.showFormError(msg);
			}
		}

	}
	
	
	private interface MetaUpdated
	{
		public void metadataUpdated(String key, String value);
	}
}
