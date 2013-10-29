/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component showing a list of elements with add and remove capabilities.
 * It is possible to configure minimum and maximum number of elements.
 *  
 * @author K. Benedyczak
 */
public class ListOfEmbeddedElements<T> extends VerticalLayout
{
	private UnityMessageSource msg;
	private EditorProvider<T> editorProvider;
	private int min = 0;
	private int max = Integer.MAX_VALUE;
	private boolean showLine;
	private Label lonelyLabel;
	private Button lonelyAdd;
	private List<Entry> components;
	
	public ListOfEmbeddedElements(UnityMessageSource msg, EditorProvider<T> editorProvider,
			int min, int max, boolean showLine)
	{
		this("", msg, editorProvider, min, max, showLine);
	}

	public ListOfEmbeddedElements(String caption, UnityMessageSource msg, EditorProvider<T> editorProvider,
			int min, int max, boolean showLine)
	{
		this.msg = msg;
		this.editorProvider = editorProvider;
		this.min = min;
		this.max = max;
		this.showLine = showLine;

		setMargin(true);
		setCaption(caption);
		
		components = new ArrayList<>();
		lonelyLabel = new Label();
		lonelyAdd = new Button();
		lonelyAdd.setIcon(Images.add.getResource());
		lonelyAdd.setDescription(msg.getMessage("add"));
		lonelyAdd.setStyleName(Reindeer.BUTTON_SMALL);
		lonelyAdd.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				addEntry(null, null);
			}
		});
		HorizontalLayout lonelyBar = new HorizontalLayout(lonelyLabel, lonelyAdd);
		lonelyBar.setSpacing(true);
		addComponent(lonelyBar);
		for (int i=0; i<min; i++)
			addEntry(null, null);
	}

	/**
	 * Sets label which is displayed before the button to add the <b>first</b> value.
	 * By default this label is empty.
	 * @param label
	 */
	public void setLonelyLabel(String label)
	{
		lonelyLabel.setValue(label);
	}
	
	public void setEntries(Collection<T> values)
	{
		clearContents();
		for (T value: values)
			addEntry(value, null);
	}
	
	public void addEntry(T value, Entry after)
	{
		lonelyAdd.setVisible(false);
		lonelyLabel.setVisible(false);
		Entry component;
		if (after == null)
		{
			component = new Entry(value, 0);
			components.add(0, component);
			addComponent(component, 0);
		} else
		{
			int i = components.indexOf(after);
			component = new Entry(value, i+1);
			components.add(i+1, component);
			addComponent(component, i+1);
		}
		for (int i=0; i<components.size(); i++)
			components.get(i).refresh(i);
	}
	
	private void remove(Entry e)
	{
		components.remove(e);
		removeComponent(e);
		for (int i=0; i<components.size(); i++)
			components.get(i).refresh(i);
		if (components.size() == 0)
		{
			lonelyAdd.setVisible(true);
			lonelyLabel.setVisible(true);
		}
	}
	
	public void clearContents()
	{
		components.clear();
		removeAllComponents();
		addComponent(lonelyAdd);
	}
	
	public List<T> getElements() throws FormValidationException
	{
		List<T> ret = new ArrayList<>(components.size());
		for (Entry e: components)
			ret.add(e.getElement());
		return ret;
	}
	
	public interface Editor<T>
	{
		/**
		 * @param value initial value or null if not set
		 * @return
		 */
		public Component getEditorComponent(T value, int position);
		
		/**
		 * Called when the position of the edited value changed. 
		 * @param position
		 */
		public void setEditedComponentPosition(int position);
		
		/**
		 * @return the value from editor.
		 * @throws FormValidationException
		 */
		public T getValue() throws FormValidationException;
	}

	public interface EditorProvider<T>
	{
		public Editor<T> getEditor();
	}
	
	private class Entry extends VerticalLayout
	{
		private Button add;
		private Button remove;
		private Label hr;
		private Editor<T> editor;
		
		public Entry(T elementV, int position)
		{
			setSpacing(true);

			if (showLine)
			{
				hr = new Label("<hr>", ContentMode.HTML);
				addComponent(hr);
			}
			
			editor = editorProvider.getEditor();
			Component c = editor.getEditorComponent(elementV, position);
			addComponent(c);
			
			add = new Button();
			add.setIcon(Images.add.getResource());
			add.setDescription(msg.getMessage("add"));
			add.setStyleName(Reindeer.BUTTON_SMALL);
			add.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					addEntry(null, Entry.this);
				}
			});
			remove = new Button();
			remove.setIcon(Images.delete.getResource());
			remove.setDescription(msg.getMessage("remove"));
			remove.setStyleName(Reindeer.BUTTON_SMALL);
			remove.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					remove(Entry.this);
				}
			});
			
			HorizontalLayout hl = new HorizontalLayout();
			hl.setSpacing(true);
			hl.addComponents(add, remove);
			
			addComponent(hl);
		}
		
		public T getElement() throws FormValidationException
		{
			return editor.getValue();
		}
		
		public void refresh(int newPosition)
		{
			if (showLine)
				hr.setVisible(ListOfEmbeddedElements.this.components.get(0) != this);
			remove.setVisible(ListOfEmbeddedElements.this.components.size() > min);
			add.setVisible(ListOfEmbeddedElements.this.components.size() < max);
			editor.setEditedComponentPosition(newPosition);
		}
	}

}
