/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Maintains a list of values. The values are displayed as labels. Additionally it is possible 
 * to have a remove and edit buttons with custom action handlers for each entry.
 * <p>
 * This is not a standalone component, this object can insert itself to a given layout.
 * 
 * @author K. Benedyczak
 */
public class ListOfElementsStub<T>
{
	private UnityMessageSource msg;
	private List<Entry> components;
	private LabelConverter<T> labelConverter;
	private EditHandler<T> editHandler;
	private RemoveHandler<T> removeHandler;
	private boolean addSeparatorLine;
	private AbstractOrderedLayout parent;
	
	public ListOfElementsStub(UnityMessageSource msg, 
			AbstractOrderedLayout parent, LabelConverter<T> labelConverter)
	{
		this.msg = msg;
		this.parent = parent;
		this.labelConverter = labelConverter;
		this.components = new ArrayList<>();
	}
	
	public ListOfElementsStub(UnityMessageSource msg, 
			AbstractOrderedLayout parent)
	{
		this(msg, parent, new DefaultLabelConverter<T>());
	}
	
	public void setEditHandler(EditHandler<T> editHandler)
	{
		this.editHandler = editHandler;
	}

	public boolean isAddSeparatorLine()
	{
		return addSeparatorLine;
	}

	public void setAddSeparatorLine(boolean addSeparatorLine)
	{
		this.addSeparatorLine = addSeparatorLine;
	}

	public void setRemoveHandler(RemoveHandler<T> removeHandler)
	{
		this.removeHandler = removeHandler;
	}

	public void addEntry(T entry)
	{
		Entry component = new Entry(entry);
		components.add(component);
		parent.addComponent(component);
	}
	
	private void removeEntry(Entry entry)
	{
		components.remove(entry);
		parent.removeComponent(entry);
	}
	
	public void clearContents()
	{
		for (Entry e: components)
			removeEntry(e);
	}
	
	public List<T> getElements()
	{
		List<T> ret = new ArrayList<>(components.size());
		for (Entry e: components)
			ret.add(e.getElement());
		return ret;
	}
	
	public int size()
	{
		return components.size();
	}
	
	private class Entry extends CustomComponent
	{
		private T element;
		private Label label;
		private Button edit;
		private Button remove;
		
		public Entry(T elementV)
		{
			HorizontalLayout cont = new HorizontalLayout();
			
			this.element = elementV;
			cont.setSpacing(true);
			if (editHandler != null)
			{
				edit = new Button();
				edit.setIcon(Images.edit.getResource());
				edit.setDescription(msg.getMessage("edit"));
				edit.setStyleName(Reindeer.BUTTON_SMALL);
				edit.addClickListener(new Button.ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						element = editHandler.edit(element);
						parent.removeComponent(label);
						parent.addComponent(labelConverter.toLabel(element));
					}
				});
				cont.addComponent(edit);
			}
			if (removeHandler != null)
			{
				remove = new Button();
				remove.setIcon(Images.delete.getResource());
				remove.setDescription(msg.getMessage("remove"));
				remove.setStyleName(Reindeer.BUTTON_SMALL);
				remove.addClickListener(new Button.ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						boolean done = removeHandler.remove(element);
						if (done)
							removeEntry(Entry.this);
					}
				});
				cont.addComponent(remove);
			}
			cont.addComponent(labelConverter.toLabel(element));
			VerticalLayout main = new VerticalLayout(cont);
			if (addSeparatorLine)
			{
				main.addComponent(HtmlTag.hr());
			}
			main.setSpacing(true);
			setCompositionRoot(main);
		}
		
		public T getElement()
		{
			return element;
		}
	}

	/**
	 * Converts generic object to string with its toString method.
	 * @author K. Benedyczak
	 * @param <T>
	 */
	public static class DefaultLabelConverter<T> implements LabelConverter<T>
	{
		@Override
		public Label toLabel(T value)
		{
			return new Label(value.toString());
		}
	}
	
	public interface LabelConverter<T>
	{
		public Label toLabel(T value);
	}
	
	public interface EditHandler<T>
	{
		public T edit(T value);
	}

	public interface RemoveHandler<T>
	{
		public boolean remove(T value);
	}
}
