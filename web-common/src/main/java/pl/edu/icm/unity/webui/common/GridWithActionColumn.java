/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.components.grid.DetailsGenerator;
import com.vaadin.ui.components.grid.GridRowDragger;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.grid.FilterableGrid;

/**
 * Grid with actions column.
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public class GridWithActionColumn<T> extends Grid<T> implements FilterableGrid<T>
{
	private UnityMessageSource msg;
	private List<T> contents;
	private ListDataProvider<T> dataProvider;
	private Column<T, HorizontalLayout> actionColumn;

	private Collection<SerializablePredicate<T>> filters;
	private List<SingleActionHandler<T>> actionHandlers;
	private List<SingleActionHandler<T>> hamburgerActionHandlers;
	private boolean heightByRows;
	private int minHeightByRow = 2;
	

	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers)
	{
		this(msg, actionHandlers, true, true);
	}

	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers,
			boolean enableDrag)
	{
		this(msg, actionHandlers, enableDrag, true);
	}

	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers,
			boolean enableDrag, boolean heightByRows)
	{
		this.msg = msg;
		this.actionHandlers = actionHandlers;
		this.hamburgerActionHandlers = new ArrayList<>();
		this.heightByRows = heightByRows;

		contents = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		setSizeFull();

		refreshActionColumn();
		if (enableDrag)
		{
			new GridRowDragger<>(this);
		}

		setSelectionMode(SelectionMode.NONE);
		setStyleName("u-gridWithAction");
		refreshHeight();
		filters = new ArrayList<>();
	}

	public void setMultiSelect(boolean multi)
	{
		setSelectionMode(multi ? SelectionMode.MULTI : SelectionMode.SINGLE);
		GridSelectionSupport.installClickListener(this);
		if (multi)
		{
			addStyleName("u-gridWithActionMulti");
		} else
		{
			removeStyleName("u-gridWithActionMulti");
		}
	}

	public void replaceElement(T old, T newElement)
	{
		contents.set(contents.indexOf(old), newElement);
		dataProvider.refreshAll();
		deselectAll();
	}

	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.refreshAll();
		deselectAll();
		refreshHeight();
	}

	public void addElement(int index, T el)
	{
		contents.add(index, el);
		dataProvider.refreshAll();
		deselectAll();
		refreshHeight();
	}

	@Override
	public void setItems(Collection<T> items)
	{
		contents = new ArrayList<>();
		if (items != null)
		{
			contents.addAll(items);
		}
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		updateFilters();
		deselectAll();
		refreshHeight();
	}

	public List<T> getElements()
	{
		return contents;
	}

	public void removeElement(T el)
	{
		contents.remove(el);
		dataProvider.refreshAll();
		deselectAll();
		refreshHeight();
	}

	public void setHeightByRows(boolean byRow)
	{
		heightByRows = byRow;
		refreshHeight();
	}

	private void refreshHeight()
	{
		if (heightByRows)
		{
			setHeightByRows(contents.size() > minHeightByRow ? contents.size() : minHeightByRow);
		}
	}

	public Column<T, String> addSortableColumn(ValueProvider<T, String> valueProvider, String caption,
			int expandRatio)
	{
		Column<T, String> column = addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(true);
		refreshActionColumn();
		return column;
	}

	public Column<T, String> addColumn(ValueProvider<T, String> valueProvider, String caption, int expandRatio)
	{
		Column<T, String> column = addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false);
		refreshActionColumn();
		return column;
	}

	public Column<T, Component> addComponentColumn(ValueProvider<T, Component> valueProvider, String caption,
			int expandRatio)
	{
		Column<T, Component> column = addComponentColumn(valueProvider).setCaption(caption)
				.setExpandRatio(expandRatio).setResizable(false).setSortable(false);
		refreshActionColumn();
		return column;
	}

	public Column<T, CheckBox> addCheckboxColumn(ValueProvider<T, Boolean> valueProvider, String caption,
			int expandRatio)
	{
		Column<T, CheckBox> column = addComponentColumn(t -> createReadOnlyCheckBox(valueProvider.apply(t)))
				.setCaption(caption).setExpandRatio(expandRatio).setResizable(false).setSortable(true)
				.setComparator((t1, t2) -> {
					return valueProvider.apply(t1).compareTo(valueProvider.apply(t2));
				});
		refreshActionColumn();
		return column;
	}

	private CheckBox createReadOnlyCheckBox(boolean value)
	{
		CheckBox check = new CheckBox();
		check.setValue(value);
		check.setReadOnly(true);
		return check;
	}

	public Column<T, Component> addShowDetailsColumn(DetailsGenerator<T> generator)
	{
		setDetailsGenerator(generator);
		Column<T, Component> showDetailsColumn = addComponentColumn(t -> getShowHideDetailsButton(t));
		showDetailsColumn.setResizable(false);
		showDetailsColumn.setExpandRatio(0);
		showDetailsColumn.setSortable(false);
		return showDetailsColumn;
	}

	public void addActionHandler(SingleActionHandler<T> actionHandler)
	{
		actionHandlers.add(actionHandler);
		refreshActionColumn();
	}

	public void addByClickDetailsComponent(DetailsGenerator<T> generator)
	{
		setDetailsGenerator(generator);
		addItemClickListener(e -> {
			setDetailsVisible(e.getItem(), !isDetailsVisible(e.getItem()));
		});
	}

	private HorizontalLayout getShowHideDetailsButton(T t)
	{
		boolean isDetailsVisiable = isDetailsVisible(t);
		Button showHide = new Button();
		showHide.setIcon(isDetailsVisiable ? Images.caret_down.getResource() : Images.caret_right.getResource());
		showHide.setDescription(isDetailsVisiable ? msg.getMessage("GridWithActionColumn.hideDetails")
				: msg.getMessage("GridWithActionColumn.showDetails"));
	
		showHide.setStyleName(Styles.vButtonSmall.toString());
		showHide.addStyleName(Styles.showHideButton.toString());
		
		showHide.addClickListener(e -> {

			setDetailsVisible(t, !isDetailsVisiable);
		});

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.addComponent(showHide);
		wrapper.setMargin(false);
		wrapper.setSpacing(false);
		wrapper.setWidth(100, Unit.PERCENTAGE);

		return wrapper;
	}

	public void refreshActionColumn()
	{
		if (actionColumn != null)
		{
			removeColumn(actionColumn);
		}

		actionColumn = addComponentColumn(t -> getButtonComponent(new HashSet<>(Arrays.asList(t))))
				.setCaption(msg.getMessage("actions")).setMinimumWidth(80);
		actionColumn.setResizable(false);
		actionColumn.setExpandRatio(0);
		actionColumn.setSortable(false);

	}

	public void addHamburgerActions(List<SingleActionHandler<T>> handlers)
	{
		handlers.forEach(h -> this.hamburgerActionHandlers.add(h));
		refreshActionColumn();
	}

	private HorizontalLayout getButtonComponent(Set<T> target)
	{
		HorizontalLayout actions = new HorizontalLayout();
		actions.setMargin(false);
		actions.setSpacing(false);
		actions.setWidth(100, Unit.PERCENTAGE);
		actions.addStyleName(Styles.smallSpacing.toString());

		for (SingleActionHandler<T> handler : actionHandlers)
		{
			Button actionButton = new Button();
			actionButton.setStyleName(Styles.vButtonSmall.toString());
			actionButton.setIcon(handler.getIcon());
			actionButton.setDescription(handler.getCaption());
			actionButton.addClickListener(e -> handler.handle(target));
			actionButton.setEnabled(handler.isEnabled(target));
			actionButton.setVisible(handler.isVisible(target));
			actions.addComponent(actionButton);
			actions.setComponentAlignment(actionButton, Alignment.TOP_CENTER);
		}
		if (hamburgerActionHandlers != null && !hamburgerActionHandlers.isEmpty())
		{
			HamburgerMenu<T> menu = new HamburgerMenu<T>();
			menu.setTarget(target);
			menu.addActionHandlers(hamburgerActionHandlers);
			menu.addStyleName(SidebarStyles.sidebar.toString());
			actions.addComponent(menu);
			actions.setComponentAlignment(menu, Alignment.TOP_CENTER);
		}

		return actions;
	}
	
	@Override
	public void addFilter(SerializablePredicate<T> filter)
	{
		if (!filters.contains(filter))
			filters.add(filter);
		updateFilters();
	}
	@Override
	public void removeFilter(SerializablePredicate<T> filter)
	{
		if (filters.contains(filter))
			filters.remove(filter);
		updateFilters();
	}
	@Override
	public void clearFilters()
	{
		dataProvider.clearFilters();
	}
	
	private void updateFilters()
	{
		dataProvider.clearFilters();
		for (SerializablePredicate<T> p : filters)
			dataProvider.addFilter(p);
	}
	
	public void setMinHeightByRow(int minRow)
	{
		this.minHeightByRow = minRow;
	}

}