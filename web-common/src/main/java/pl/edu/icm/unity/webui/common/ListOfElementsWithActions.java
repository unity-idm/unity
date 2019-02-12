/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * UI Component presenting a list of objects in a table with multiple colums.
 * Additionally entries may have actions attached, which are displayed as
 * buttons in each line.
 * 
 * @author P.Piernik
 */
public class ListOfElementsWithActions<T> extends CustomComponent
{
	List<Column<T>> columns;
	ActionColumn<T> actionColumn;

	private List<Entry> components;
	private boolean addSeparatorLine;
	private VerticalLayout main;

	public ListOfElementsWithActions()
	{

		this(Arrays.asList(new Column<>(null, t -> new Label(t.toString()))), null);

	}

	public ListOfElementsWithActions(LabelConverter<T> labelConverter)
	{

		this(Arrays.asList(new Column<>(null, labelConverter)), null);
	}

	public ListOfElementsWithActions(List<Column<T>> columns, ActionColumn<T> actionColumn)
	{

		this.columns = columns;
		this.actionColumn = actionColumn;

		this.components = new ArrayList<>();
		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.setId("ListOfElements");
		setCompositionRoot(main);
		addHeader();
	}

	public void addEntry(T entry)
	{
		Entry component = new Entry(entry);
		components.add(component);
		main.addComponent(component);
	}

	public void removeEntry(Entry entry)
	{
		components.remove(entry);
		main.removeComponent(entry);
	}

	public void removeEntry(T element)
	{
		Entry entry = getElement(element);
		if (entry != null)
			removeEntry(entry);
	}

	public Entry getElement(T element)
	{
		for (Entry e : components)
		{
			if (e.getElement() == element)
				return e;
		}
		return null;
	}

	public void clearContents()
	{
		components.clear();
		main.removeAllComponents();
	}

	public List<T> getElements()
	{
		List<T> ret = new ArrayList<>(components.size());
		for (Entry e : components)
			ret.add(e.getElement());
		return ret;
	}

	public int size()
	{
		return components.size();
	}

	public void setAddSeparatorLine(boolean addSeparatorLine)
	{
		this.addSeparatorLine = addSeparatorLine;
	}

	private void addHeader()
	{
		HorizontalLayout columnsL = new HorizontalLayout(); 
		columnsL.setWidth(100, Unit.PERCENTAGE);
		columnsL.setMargin(false);
		columnsL.setSpacing(false);
		
		boolean add = false;
		for (Column<T> c : columns)
		{
			Label titleL = new Label();
			titleL.setStyleName(Styles.captionBold.toString());
			if (c.headerLabel != null)
			{
				add = true;
				titleL.setValue(c.headerLabel);
			}
			columnsL.addComponent(titleL);
			titleL.setWidth(100, Unit.PERCENTAGE);
			columnsL.setExpandRatio(titleL, c.expandRatio);
		}

		HorizontalLayout buttonsL = new HorizontalLayout();
		buttonsL.setMargin(false);
		buttonsL.setSpacing(false);
		
		Label actionL = new Label();
		actionL.setStyleName(Styles.captionBold.toString());
		actionL.setWidth(100, Unit.PERCENTAGE);
		if (actionColumn != null && actionColumn.headerLabel != null)
		{
			actionL.setValue(actionColumn.headerLabel);	
			add = true;
		}
		buttonsL.addComponent(actionL);
		
		if (add)
		{
			main.addComponent(getEntryLine(columnsL, buttonsL), 0);
			main.addComponent(HtmlTag.horizontalLine());
		}

	}

	private Component getEntryLine(HorizontalLayout components, HorizontalLayout buttons)
	{
		if (actionColumn != null)
		{
			if (actionColumn.position == Position.Right)
			{
				components.addComponent(buttons);
				components.setComponentAlignment(buttons, Alignment.TOP_RIGHT);

			} else
			{
				components.addComponent(buttons, 0);
				components.setComponentAlignment(buttons, Alignment.TOP_LEFT);
			}
			components.setExpandRatio(buttons, actionColumn.expandRatio);
		}

		VerticalLayout main = new VerticalLayout(components);
		main.setSpacing(false);
		main.setMargin(false);

		if (addSeparatorLine)
		{
			main.addComponent(HtmlTag.horizontalLine());
		}
		return main;
	}

	private class Entry extends CustomComponent
	{
		private T element;

		public Entry(T elementV)
		{
			this.element = elementV;

			setCompositionRoot(getEntryLine(buildColumnsLayout(element), buildActionColumnLayout(element)));
		}

		public T getElement()
		{
			return element;
		}
	}

	private HorizontalLayout buildColumnsLayout(T element)
	{
		HorizontalLayout labels = new HorizontalLayout();
		labels.setWidth(100, Unit.PERCENTAGE);
		labels.setMargin(false);
		for (Column<T> column : columns)
		{
			Component l = column.labelConverter.toLabel(element);
			l.setWidth(100, Unit.PERCENTAGE);
			labels.addComponent(l);
			labels.setExpandRatio(l, column.expandRatio);
			labels.setComponentAlignment(l, Alignment.TOP_LEFT);
		}
		return labels;
	}

	private HorizontalLayout buildActionColumnLayout(T element)
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setMargin(false);
		buttons.setSpacing(false);
		if (actionColumn != null)
		{
			for (SingleActionHandler<T> handler : actionColumn.actionHandlers)
			{

				Set<T> elementsSet = new HashSet<>();
				elementsSet.add(element);
				if (handler.isVisible(elementsSet))
				{
					Button actionButton = new Button();
					actionButton.setIcon(handler.getIcon());
					actionButton.setDescription(handler.getCaption());
					actionButton.setStyleName(Styles.vButtonSmall.toString());
					actionButton.addClickListener(new Button.ClickListener()
					{
						@Override
						public void buttonClick(ClickEvent event)
						{
							handler.handle(Stream.of(element).collect(Collectors.toSet()));
						}
					});
					actionButton.setEnabled(handler.isEnabled(elementsSet));
					buttons.addComponent(actionButton);
				}
			}
		}
		return buttons;
	}

	public interface LabelConverter<T>
	{
		public Component toLabel(T value);
	}

	private static class BaseColumn
	{
		public final String headerLabel;
		public final int expandRatio;

		public BaseColumn(String headerLabel, int expandRatio)
		{
			this.headerLabel = headerLabel;
			this.expandRatio = expandRatio;
		}
	}

	public static class Column<T> extends BaseColumn
	{
		public final LabelConverter<T> labelConverter;

		public Column(String headerLabel, LabelConverter<T> labelConverter)
		{
			super(headerLabel, 1);
			this.labelConverter = labelConverter;
		}

		public Column(String headerLabel, LabelConverter<T> labelConverter, int expandRatio)
		{
			super(headerLabel, expandRatio);
			this.labelConverter = labelConverter;
		}
	}

	public static class ActionColumn<T> extends BaseColumn
	{
		public enum Position
		{
			Left, Right
		};

		public final List<SingleActionHandler<T>> actionHandlers;
		public final Position position;

		public ActionColumn(String headerLabel, List<SingleActionHandler<T>> actionHandlers,
				int expandRatio, Position position)
		{
			super(headerLabel, expandRatio);
			this.actionHandlers = Collections.unmodifiableList(
					actionHandlers == null ? Collections.emptyList() : actionHandlers);
			this.position = position;
		}
	}

}
