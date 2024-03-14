/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.imunity.vaadin.elements.CssClassNames.POINTER;

public class ListOfEmbeddedElementsStub<T>
{
	private final MessageSource msg;
	private final EditorProvider<T> editorProvider;
	private final int min;
	private final int max;
	private final boolean showLine;
	private final HorizontalLayout lonelyBar;
	private final Button lonelyAdd = new Button();
	private final List<Entry> components;
	private final ComponentsGroup group;
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

		Icon lonelyAdd = VaadinIcon.PLUS_CIRCLE_O.create();
		lonelyAdd.addClassName(POINTER.name());
		lonelyAdd.setTooltipText(msg.getMessage("add"));
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
			group.addComponent(uiComponents[0], start+parentOffset, after.addRemoveBar);
			for (int j=1; j<uiComponents.length; j++)
				group.addComponent(uiComponents[j], start+j+parentOffset, uiComponents[j-1]);
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
		private final Icon add;
		private final Icon remove;
		private Hr hr;
		private final Editor<T> editor;
		private final ComponentsContainer cc;
		private final HorizontalLayout addRemoveBar;
		
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
			
			add = VaadinIcon.PLUS_CIRCLE_O.create();
			add.setClassName(CssClassNames.SMALL_ICON.getName());
			add.setTooltipText(msg.getMessage("add"));
			add.addClickListener(event -> addEntry(null, Entry.this));
			remove = VaadinIcon.TRASH.create();
			remove.setClassName(CssClassNames.SMALL_ICON.getName());
			remove.setTooltipText(msg.getMessage("remove"));
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
