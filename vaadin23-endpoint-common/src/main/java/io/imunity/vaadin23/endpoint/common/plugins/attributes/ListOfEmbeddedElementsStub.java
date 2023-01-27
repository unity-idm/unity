/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.plugins.attributes;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.vaadin23.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListOfEmbeddedElementsStub<T>
{
	private MessageSource msg;
	private EditorProvider<T> editorProvider;
	private int min;
	private int max;
	private boolean showLine;
	private HorizontalLayout lonelyBar;
	private Button lonelyAdd = new Button();
	private List<Entry> components;
	private ComponentsGroup group;
	private Runnable valueChangeListener;
	
	
	public ListOfEmbeddedElementsStub(MessageSource msg, EditorProvider<T> editorProvider,
	                                  int min, int max, boolean showLine)
	{
		this.msg = msg;
		this.editorProvider = editorProvider;
		this.min = min;
		this.max = max;
		this.showLine = showLine;
		this.group = new ComponentsGroup();

		components = new ArrayList<>();
		
		Button lonelyAdd = new Button();
		lonelyAdd.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		lonelyAdd.getElement().setProperty("title" ,msg.getMessage("add"));
		lonelyAdd.addClickListener(event -> addEntry(null, null));
		
		lonelyBar = new HorizontalLayout(lonelyAdd);
		lonelyBar.setSpacing(true);
		lonelyBar.setMargin(true);
		group.addComponent(lonelyBar);
		for (int i=0; i<min; i++)
			addEntry(null, null);
	}

	public void setValueChangeListener(Runnable listener)
	{
		valueChangeListener = listener;
	}

	public void setLonelyLabel(String label)
	{
		lonelyAdd.setText(label);
	}
	
	public void refresh()
	{
		for (int i=0; i<components.size(); i++)
			components.get(i).refresh(i);
	}
	
	public void setEntries(Collection<? extends T> values)
	{
		clearContents();
		if (values.isEmpty())
			lonelyBar.setVisible(true);
		Entry e = null;
		for (T value: values)
			e = addEntry(value, e);
	}
	
	public Entry addEntry(T value, Entry after)
	{
		lonelyBar.setVisible(false);
		int parentOffset = group.getComponentIndex(lonelyBar)+1;
		Entry entry;
		if (after == null)
		{
			entry = new Entry(value, 0);
			components.add(0, entry);
			Component[] uiComponents = entry.getContents().getComponents();
			for (int i=0; i<uiComponents.length; i++)
				group.addComponent(uiComponents[i], i+parentOffset);
		} else
		{
			int i = components.indexOf(after);
			int start = 0;
			for (int j=0; j<=i; j++)
				start += components.get(j).getContents().getComponents().length;
			entry = new Entry(value, i+1);
			components.add(i+1, entry);
			
			Component[] uiComponents = entry.getContents().getComponents();
			for (int j=0; j<uiComponents.length; j++)
				group.addComponent(uiComponents[j], start+j+parentOffset);
		}
		
		for (int i=0; i<components.size(); i++)
			components.get(i).refresh(i);
		if (valueChangeListener != null)
			valueChangeListener.run();
		return entry;
	}
	
	private void remove(Entry e)
	{
		components.remove(e);
		Component[] uiComponents = e.getContents().getComponents();
		for (Component c: uiComponents)
			group.removeComponent(c);
		for (int i=0; i<components.size(); i++)
			components.get(i).refresh(i);
		if (components.size() == 0)
			lonelyBar.setVisible(true);
		if (valueChangeListener != null)
			valueChangeListener.run();
	}
	
	public void clearContents()
	{
		for (Entry e: components)
		{
			for (Component c: e.getContents().getComponents())
				group.removeComponent(c);
		}
		components.clear();
		lonelyBar.setVisible(false);
		if (valueChangeListener != null)
			valueChangeListener.run();
	}
	
	public void resetContents()
	{
		clearContents();
		lonelyBar.setVisible(true);
	}
	
	public List<T> getElements() throws FormValidationException
	{
		List<T> ret = new ArrayList<>(components.size());
		for (Entry e: components)
			ret.add(e.getElement());
		return ret;
	}
	
	public ComponentsGroup getComponentsGroup()
	{
		return group;
	}


	public interface Editor<T>
	{
		ComponentsContainer getEditorComponent(T value, int position);
		void setEditedComponentPosition(int position);
		T getValue() throws FormValidationException;
	}

	public interface EditorProvider<T>
	{
		Editor<T> getEditor();
	}
	
	public class Entry
	{
		private Button add;
		private Button remove;
		private Hr hr;
		private Editor<T> editor;
		private ComponentsContainer cc;
		private HorizontalLayout addRemoveBar;
		
		public Entry(T elementV, int position)
		{
			cc = new ComponentsContainer();
			if (showLine)
			{
				hr = new Hr();
				cc.add(hr);
			}
			
			editor = editorProvider.getEditor();
			ComponentsContainer c = editor.getEditorComponent(elementV, position);
			cc.add(c.getComponents());
			
			add = new Button();
			add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			add.getElement().setProperty("title", msg.getMessage("add"));
			add.addClickListener(event -> addEntry(null, Entry.this));
			remove = new Button();
			remove.setIcon(VaadinIcon.TRASH.create());
			remove.getElement().setProperty("title", msg.getMessage("remove"));
			remove.addClickListener(event -> remove(Entry.this));
			
			addRemoveBar = new HorizontalLayout();
			addRemoveBar.setSpacing(true);
			addRemoveBar.setMargin(false);
			addRemoveBar.add(add, remove);
			cc.add(addRemoveBar);
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
			boolean addVisible = ListOfEmbeddedElementsStub.this.components.size() < max;
			boolean removeVisible = ListOfEmbeddedElementsStub.this.components.size() > min;
			remove.setVisible(removeVisible);
			add.setVisible(addVisible);
			addRemoveBar.setVisible(addVisible || removeVisible);
			
			editor.setEditedComponentPosition(newPosition);
		}
	}

}
