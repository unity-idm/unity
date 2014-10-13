/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component stub showing a list of elements with add and remove capabilities.
 * It is possible to configure minimum and maximum number of elements.
 * <p>
 * This class is not a full component. Instead it allows to add its contents to another container
 * 
 *  
 * @author K. Benedyczak
 */
public class ListOfEmbeddedElementsStub<T>
{
	private UnityMessageSource msg;
	private EditorProvider<T> editorProvider;
	private int min = 0;
	private int max = Integer.MAX_VALUE;
	private boolean showLine;
	private HorizontalLayout lonelyBar;
	private List<Entry> components;
	
	private AbstractOrderedLayout parent;
	
	public ListOfEmbeddedElementsStub(UnityMessageSource msg, EditorProvider<T> editorProvider,
			int min, int max, boolean showLine, AbstractOrderedLayout parent)
	{
		this.parent = parent;
		this.msg = msg;
		this.editorProvider = editorProvider;
		this.min = min;
		this.max = max;
		this.showLine = showLine;

		components = new ArrayList<>();
		Button lonelyAdd = new Button();
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
		lonelyBar = new HorizontalLayout(lonelyAdd);
		lonelyBar.setSpacing(true);
		lonelyBar.setMargin(new MarginInfo(true, false, true, false));
		parent.addComponent(lonelyBar);
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
		lonelyBar.setCaption(label);
	}
	
	public void setEntries(Collection<T> values)
	{
		clearContents();
		Entry e = null;
		for (T value: values)
			e = addEntry(value, e);
	}
	
	public Entry addEntry(T value, Entry after)
	{
		lonelyBar.setVisible(false);
		int parentOffset = parent.getComponentIndex(lonelyBar)+1;
		Entry entry;
		if (after == null)
		{
			entry = new Entry(value, 0);
			components.add(0, entry);
			Component[] uiComponents = entry.getContents().getComponents(); 
			for (int i=0; i<uiComponents.length; i++)
				parent.addComponent(uiComponents[i], i+parentOffset);
		} else
		{
			int i = components.indexOf(after);
			entry = new Entry(value, i+1);
			components.add(i+1, entry);
			
			Component[] uiComponents = entry.getContents().getComponents();
			int start = (i+1)*uiComponents.length;
			for (int j=0; j<uiComponents.length; j++)
				parent.addComponent(uiComponents[j], start+j+parentOffset);
		}
		
		for (int i=0; i<components.size(); i++)
			components.get(i).refresh(i);
		return entry;
	}
	
	private void remove(Entry e)
	{
		components.remove(e);
		Component[] uiComponents = e.getContents().getComponents();
		for (Component c: uiComponents)
			parent.removeComponent(c);
		for (int i=0; i<components.size(); i++)
			components.get(i).refresh(i);
		if (components.size() == 0)
			lonelyBar.setVisible(true);
	}
	
	public void clearContents()
	{
		for (Entry e: components)
		{
			for (Component c: e.getContents().getComponents())
				parent.removeComponent(c);
		}
		components.clear();
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
		public ComponentsContainer getEditorComponent(T value, int position);
		
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
	
	public class Entry
	{
		private Button add;
		private Button remove;
		private Label hr;
		private Editor<T> editor;
		private ComponentsContainer cc;
		
		public Entry(T elementV, int position)
		{
			cc = new ComponentsContainer();
			if (showLine)
			{
				hr = HtmlTag.hr();
				cc.add(hr);
			}
			
			editor = editorProvider.getEditor();
			ComponentsContainer c = editor.getEditorComponent(elementV, position);
			cc.add(c.getComponents());
			
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
			
			cc.add(hl);
		}
		
		private ComponentsContainer getContents()
		{
			return cc;
		}
		
		public T getElement() throws FormValidationException
		{
			return editor.getValue();
		}
		
		public void refresh(int newPosition)
		{
			if (showLine)
				hr.setVisible(ListOfEmbeddedElementsStub.this.components.get(0) != this);
			remove.setVisible(ListOfEmbeddedElementsStub.this.components.size() > min);
			add.setVisible(ListOfEmbeddedElementsStub.this.components.size() < max);
			editor.setEditedComponentPosition(newPosition);
		}
	}

}
