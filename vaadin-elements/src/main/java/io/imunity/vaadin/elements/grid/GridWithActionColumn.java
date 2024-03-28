/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements.grid;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import io.imunity.vaadin.elements.ActionIconBuilder;

import java.util.*;
import java.util.function.Function;

/**
 * Grid with actions column.
 * 
 * @author P.Piernik
 *
 */
public class GridWithActionColumn<T> extends Grid<T> implements FilterableGrid<T>
{
	private final Function<String, String> msg;
	private List<T> contents;
	private GridListDataView<T> dataProvider;
	private Column<T> actionColumn;
	private T draggedItem;

	private final Collection<SerializablePredicate<T>> filters;
	private final List<SingleActionHandler<T>> actionHandlers;
	private final List<SingleActionHandler<T>> hamburgerActionHandlers;
	
	
	public GridWithActionColumn(Function<String, String> msg, List<SingleActionHandler<T>> actionHandlers)
	{
		this.msg = msg;
		this.actionHandlers = actionHandlers;
		this.hamburgerActionHandlers = new ArrayList<>();

		filters = new ArrayList<>();
		contents = new ArrayList<>();
		dataProvider = setItems(contents);
		addThemeVariants(GridVariant.LUMO_NO_BORDER);
		refreshActionColumn();
	}

	public void enableRowReordering(Runnable update)
	{
		setDropMode(GridDropMode.BETWEEN);
		setRowsDraggable(true);

		addDragStartListener(e -> draggedItem = e.getDraggedItems().get(0));

		addDropListener(e ->
		{
			T targetPerson = e.getDropTargetItem().orElse(null);
			GridDropLocation dropLocation = e.getDropLocation();
			if (targetPerson == null || draggedItem.equals(targetPerson))
				return;

			dataProvider.removeItem(draggedItem);

			if (dropLocation == GridDropLocation.BELOW)
				dataProvider.addItemAfter(draggedItem, targetPerson);
			else
				dataProvider.addItemBefore(draggedItem, targetPerson);
			update.run();
		});
		addDragEndListener(e -> draggedItem = null);
	}

	public void setMultiSelect(boolean multi)
	{
		setSelectionMode(multi ? SelectionMode.MULTI : SelectionMode.SINGLE);
	}

	public void replaceElement(T old, T newElement)
	{
		if(Objects.equals(old, newElement))
			return;
		dataProvider.addItemBefore(newElement, old);
		dataProvider.removeItem(old);
		deselectAll();
	}

	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.addItem(el);
		deselectAll();
	}

	public void addElement(int index, T el)
	{
		contents.add(index, el);
		dataProvider.addItemAfter(contents.get(index), el);
		deselectAll();
	}

	@Override
	public GridListDataView<T> setItems(Collection<T> items)
	{
		contents = new ArrayList<>();
		if (items != null)
		{
			contents.addAll(items);
		}
		dataProvider = super.setItems(items);
		updateFilters();
		deselectAll();
		return dataProvider;
	}

	public List<T> getElements()
	{
		return dataProvider.getItems().toList();
	}

	public void removeElement(T el)
	{
		contents.remove(el);
		dataProvider.removeItem(el);
		deselectAll();
	}

	public void removeAllElements()
	{
		contents.clear();
		dataProvider.refreshAll();
		deselectAll();

	}

	public void addActionHandler(int position, SingleActionHandler<T> actionHandler)
	{
		actionHandlers.add(position, actionHandler);
		refreshActionColumn();
	}

	public void setActionColumnHeader(Component component)
	{
		actionColumn.setHeader(component);
	}

	public void refreshActionColumn()
	{
		if (actionColumn != null)
		{
			removeColumn(actionColumn);
			actionColumn = null;
		}
		actionColumn = super.addComponentColumn(e -> getButtonComponent(Set.of(e)))
				.setHeader(msg.apply("actions"))
				.setTextAlign(ColumnTextAlign.END)
				.setFlexGrow(1)
				.setAutoWidth(true)
				.setResizable(false);
	}

	public void removeActionColumn()
	{
		if (actionColumn != null)
		{
			removeColumn(actionColumn);
			actionColumn = null;
		}
	}

	public void addHamburgerActions(List<SingleActionHandler<T>> handlers)
	{
		this.hamburgerActionHandlers.addAll(handlers);
		refreshActionColumn();
	}

	private HorizontalLayout getButtonComponent(Set<T> target)
	{
		HorizontalLayout horizontalLayout = new HorizontalLayout();

		for (SingleActionHandler<T> handler : actionHandlers)
		{

			Icon actionButton = new ActionIconBuilder().icon(handler.getIcon())
					.tooltipText(handler.getCaption())
					.clickListener(() -> handler.handle(target))
					.setEnable(handler.isEnabled(target))
					.setVisible(handler.isVisible(target))
					.build();
			horizontalLayout.add(actionButton);
		}
		if (hamburgerActionHandlers != null && !hamburgerActionHandlers.isEmpty())
		{
			ActionMenuWithHandlerSupport<T> actionMenu = new ActionMenuWithHandlerSupport<>();
			actionMenu.setTarget(target);
			actionMenu.addActionHandlers(hamburgerActionHandlers);
			horizontalLayout.add(actionMenu.getTarget());
		}

		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;

	}

	public Column<T> addShowDetailsColumn(Renderer<T> renderer)
	{
		setItemDetailsRenderer(renderer);
		Column<T> showDetailsColumn = addComponentColumn(this::createDetailsArrow).setFlexGrow(0)
				.setWidth("33px");
		showDetailsColumn.setResizable(false);
		showDetailsColumn.setSortable(false);

		return showDetailsColumn;
	}

	private HorizontalLayout createDetailsArrow(T entry)
	{
		Icon openIcon = VaadinIcon.ANGLE_RIGHT.create();
		Icon closeIcon = VaadinIcon.ANGLE_DOWN.create();
		openIcon.setVisible(!isDetailsVisible(entry));
		closeIcon.setVisible(isDetailsVisible(entry));
		openIcon.addClickListener(e -> setDetailsVisible(entry, true));
		closeIcon.addClickListener(e -> setDetailsVisible(entry, false));
		HorizontalLayout layout = new HorizontalLayout(openIcon, closeIcon);
		layout.setWidth(2, Unit.EM);

		return layout;
	}

	public void addFilter(SerializablePredicate<T> filter)
	{
		if (!filters.contains(filter))
			filters.add(filter);
		updateFilters();
	}

	public void removeFilter(SerializablePredicate<T> filter)
	{
		if (filters.contains(filter))
			filters.remove(filter);
		updateFilters();
	}

	public void clearFilters()
	{
		dataProvider.removeFilters();
		filters.clear();
	}

	private void updateFilters()
	{
		dataProvider.removeFilters();
		for (SerializablePredicate<T> p : filters)
			dataProvider.addFilter(p);
	}
	
	@Override
	public <V extends Component> Column<T> addComponentColumn(ValueProvider<T, V> componentProvider)
	{
		Column<T> addComponentColumn = super.addComponentColumn(componentProvider).setResizable(true).setFlexGrow(2);
		refreshActionColumn();
		return addComponentColumn;
	}

	@Override
	public Column<T> addColumn(ValueProvider<T, ?> valueProvider)
	{
		Column<T> tColumn = super.addColumn(valueProvider);
		refreshActionColumn();
		return tColumn;
	}

	public Column<T> addBooleanColumn(Function<T, Boolean> checkBox)
	{
		Column<T> addComponentColumn = super.addComponentColumn(v -> getBoolIcon(checkBox.apply(v))).setResizable(true).setFlexGrow(2);
		refreshActionColumn();
		return addComponentColumn;
	}
	
	private Icon getBoolIcon(boolean value)
	{
		return value ? VaadinIcon.CHECK_CIRCLE_O.create() : VaadinIcon.MINUS_CIRCLE_O.create();
	}

	public Column<T> getActionColumn()
	{
		return actionColumn;
	}
	
}