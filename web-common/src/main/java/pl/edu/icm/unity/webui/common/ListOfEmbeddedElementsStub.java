/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.composite.ComponentsGroup;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Component stub showing a list of elements with add and remove capabilities.
 * It is possible to configure minimum and maximum number of elements.
 * <p>
 * This class is not a full component. Instead it returns {@link ComponentsGroup} which can be added to a layout.
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
	private ComponentsGroup group;
	private Runnable valueChangeListener;
	
	
	public ListOfEmbeddedElementsStub(UnityMessageSource msg, EditorProvider<T> editorProvider,
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
		lonelyAdd.setIcon(Images.add.getResource());
		lonelyAdd.setDescription(msg.getMessage("add"));
		lonelyAdd.addStyleName(Styles.toolbarButton.toString());
		lonelyAdd.addStyleName(Styles.vButtonLink.toString());
		lonelyAdd.addClickListener(event -> addEntry(null, null));
		
		lonelyBar = new HorizontalLayout(lonelyAdd);
		lonelyBar.setSpacing(true);
		lonelyBar.setMargin(new MarginInfo(true, false, true, false));
		group.addComponent(lonelyBar);
		for (int i=0; i<min; i++)
			addEntry(null, null);
	}

	public void setValueChangeListener(Runnable listener)
	{
		valueChangeListener = listener;
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
		private HorizontalLayout addRemoveBar;
		
		public Entry(T elementV, int position)
		{
			cc = new ComponentsContainer();
			if (showLine)
			{
				hr = HtmlTag.horizontalLine();
				cc.add(hr);
			}
			
			editor = editorProvider.getEditor();
			ComponentsContainer c = editor.getEditorComponent(elementV, position);
			cc.add(c.getComponents());
			
			add = new Button();
			add.setIcon(Images.add.getResource());
			add.setDescription(msg.getMessage("add"));
			add.addStyleName(Styles.toolbarButton.toString());
			add.addStyleName(Styles.vButtonLink.toString());
			add.addClickListener(event -> addEntry(null, Entry.this));
			remove = new Button();
			remove.setIcon(Images.delete.getResource());
			remove.setDescription(msg.getMessage("remove"));
			remove.addStyleName(Styles.toolbarButton.toString());
			remove.addStyleName(Styles.vButtonLink.toString());
			remove.addClickListener(event -> remove(Entry.this));
			
			addRemoveBar = new HorizontalLayout();
			addRemoveBar.setSpacing(true);
			addRemoveBar.setMargin(false);
			addRemoveBar.addComponents(add, remove);
			addRemoveBar.addStyleName(Styles.negativeTopMargin.toString());
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
