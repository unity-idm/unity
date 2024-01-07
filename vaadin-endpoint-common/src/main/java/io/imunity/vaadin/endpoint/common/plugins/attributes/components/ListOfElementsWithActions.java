/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.components;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.grid.SingleActionHandler;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.imunity.vaadin.elements.CssClassNames.DISABLED_ICON;


public class ListOfElementsWithActions<T> extends Div
{
	private final List<Column<T>> columns;
	private final ActionColumn<T> actionColumn;

	private final List<Entry> components;
	private final VerticalLayout main;
	private boolean addSeparatorLine;

	public ListOfElementsWithActions(LabelConverter<T> labelConverter)
	{
		this(List.of(new Column<>(null, labelConverter)), null);
	}

	public ListOfElementsWithActions(List<Column<T>> columns, ActionColumn<T> actionColumn)
	{
		this.columns = columns;
		this.actionColumn = actionColumn;

		this.components = new ArrayList<>();
		main = new VerticalLayout();
		main.setPadding(false);
		main.setSpacing(false);
		add(main);
		addHeader();
	}

	public void addEntry(T entry)
	{
		Entry component = new Entry(entry);
		components.add(component);
		main.add(component);
	}

	public void removeEntry(Entry entry)
	{
		components.remove(entry);
		main.remove(entry);
	}

	public void removeEntry(T element)
	{
		ListOfElementsWithActions.Entry entry = getElement(element);
		if (entry != null)
			removeEntry(entry);
	}

	public ListOfElementsWithActions.Entry getElement(T element)
	{
		for (ListOfElementsWithActions.Entry e : components)
		{
			if (e.getComponent() == element)
				return e;
		}
		return null;
	}

	public void clearContents()
	{
		for(Entry e : components)
		{
			main.remove(e);
		}
	}

	public List<T> getElements()
	{
		List<T> ret = new ArrayList<>(components.size());
		for (Entry e : components)
			ret.add(e.getComponent());
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
		columnsL.setWidthFull();
		columnsL.setPadding(false);
		columnsL.setSpacing(false);
		
		boolean add = false;
		for (Column<T> c : columns)
		{
			Span titleL = new Span();
			if (c.headerLabel != null)
			{
				add = true;
				titleL.setText(c.headerLabel);
			}
			columnsL.add(titleL);
			titleL.setWidthFull();
		}

		HorizontalLayout buttonsL = new HorizontalLayout();
		buttonsL.setPadding(false);
		buttonsL.setSpacing(false);
		
		Span actionL = new Span();
		if (actionColumn != null && actionColumn.headerLabel != null)
		{
			actionL.setText(actionColumn.headerLabel);
			add = true;
		}
		buttonsL.add(actionL);
		
		if (add)
		{
			main.add(getEntryLine(columnsL, buttonsL));
			main.add(new Hr());
		}

	}

	private Component getEntryLine(HorizontalLayout components, HorizontalLayout buttons)
	{
		if (actionColumn != null)
		{
			if (actionColumn.position == ActionColumn.Position.Right)
			{
				components.add(buttons);

			} else
			{
				components.addComponentAtIndex(0, buttons);
			}
		}

		VerticalLayout main = new VerticalLayout(components);
		main.setSpacing(false);
		main.setPadding(false);

		if (addSeparatorLine)
		{
			main.add(new Hr());
		}
		return main;
	}

	private class Entry extends Div
	{
		private final T component;

		public Entry(T elementV)
		{
			this.component = elementV;

			add(getEntryLine(buildColumnsLayout(component), buildActionColumnLayout(component)));
		}

		public T getComponent()
		{
			return component;
		}
	}

	private HorizontalLayout buildColumnsLayout(T element)
	{
		HorizontalLayout labels = new HorizontalLayout();
		labels.setAlignItems(FlexComponent.Alignment.CENTER);
		labels.setWidthFull();
		labels.setPadding(false);
		for (Column<T> column : columns)
		{
			Component l = column.labelConverter.toLabel(element);
			labels.add(l);
		}
		return labels;
	}

	private HorizontalLayout buildActionColumnLayout(T element)
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setPadding(false);
		buttons.setSpacing(false);
		if (actionColumn != null)
		{
			for (SingleActionHandler<T> handler : actionColumn.actionHandlers)
			{

				Set<T> elementsSet = new HashSet<>();
				elementsSet.add(element);
				if (handler.isVisible(elementsSet))
				{
					Icon actionButton = handler.getIcon().create();
					actionButton.setTooltipText(handler.getCaption());
					actionButton.addClickListener(e -> handler
							.handle(Stream.of(element).collect(Collectors.toSet())));
					if(!handler.isEnabled(elementsSet))
						actionButton.addClassName(DISABLED_ICON.getName());
					buttons.add(actionButton);
				}
			}
		}
		return buttons;
	}

	public interface LabelConverter<T>
	{
		Component toLabel(T value);
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
	
	public void setPadding(boolean padding)
	{
		main.setPadding(padding);
	}

}
