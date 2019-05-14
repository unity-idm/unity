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
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.components.grid.DetailsGenerator;
import com.vaadin.ui.components.grid.GridRowDragger;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Grid with actions column.
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public class GridWithActionColumn<T> extends Grid<T>
{
	private UnityMessageSource msg;
	private List<T> contents;
	private ListDataProvider<T> dataProvider;
	private GridContextMenuSupport<T> contextMenuSupp;
	private Column<T, HorizontalLayout> actionColumn;

	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers)
	{
		this(msg, actionHandlers, true);
	}

	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers,
			boolean enableDrag)
	{
		this.msg = msg;

		contents = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		contextMenuSupp = new GridContextMenuSupport<>(this);
		for (SingleActionHandler<T> h : actionHandlers)
		{
			contextMenuSupp.addActionHandler(h);
		}

		refreshActionColumn();
		if (enableDrag)
		{
			new GridRowDragger<>(this);
		}

		setSelectionMode(SelectionMode.NONE);
		setStyleName("u-gridWithAction");
		setHeightByRows();
	}

	public void replaceElement(T old, T newElement)
	{
		contents.set(contents.indexOf(old), newElement);
		dataProvider.refreshItem(newElement);
	}

	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.refreshAll();
		setHeightByRows();
	}

	@Override
	public void setItems(Collection<T> items)
	{
		contents.clear();
		if (items != null)
		{
			contents.addAll(items);
		}

		dataProvider.refreshAll();
		setHeightByRows();
	}

	public List<T> getElements()
	{
		return contents;
	}

	public void removeElement(T el)
	{
		contents.remove(el);
		dataProvider.refreshAll();
		setHeightByRows();
	}

	private void setHeightByRows()
	{
		setHeightByRows(contents.size() > 2 ? contents.size() : 2);
	}

	public GridWithActionColumn<T> addColumn(ValueProvider<T, String> valueProvider, String caption,
			int expandRatio)
	{
		addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio).setResizable(false)
				.setSortable(false);
		refreshActionColumn();
		return this;
	}

	public GridWithActionColumn<T> addComponentColumn(ValueProvider<T, Component> valueProvider, String caption,
			int expandRatio)
	{
		addComponentColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio).setResizable(false)
				.setSortable(false);
		refreshActionColumn();
		return this;
	}

	public void addActionHandler(SingleActionHandler<T> actionHandler)
	{
		contextMenuSupp.addActionHandler(actionHandler);
		refreshActionColumn();
	}

	public void addDetailsComponent(DetailsGenerator<T> generator)
	{
		setDetailsGenerator(generator);
		addItemClickListener(e -> {
			setDetailsVisible(e.getItem(), !isDetailsVisible(e.getItem()));
		});
	}

	public List<SingleActionHandler<T>> getActionHandlers()
	{
		return contextMenuSupp.getActionHandlers();
	}

	public void refreshActionColumn()
	{
		if (actionColumn != null)
		{
			removeColumn(actionColumn);
		}

		actionColumn = addComponentColumn(t -> getButtonComponent(new HashSet<>(Arrays.asList(t))))
				.setCaption(msg.getMessage("actions"));
		actionColumn.setResizable(false);
		actionColumn.setExpandRatio(0);
		actionColumn.setSortable(false);

	}

	private HorizontalLayout getButtonComponent(Set<T> target)
	{
		HorizontalLayout actions = new HorizontalLayout();
		actions.setMargin(false);
		actions.setSpacing(false);
		actions.setWidth(100, Unit.PERCENTAGE);

		for (SingleActionHandler<T> handler : contextMenuSupp.getActionHandlers())
		{
			Button actionButton = new Button();
			actionButton.setStyleName(Styles.vButtonSmall.toString());
			actionButton.setIcon(handler.getIcon());
			actionButton.setDescription(handler.getCaption());
			actionButton.addClickListener(e -> handler.handle(target));
			actionButton.setEnabled(handler.isEnabled(target));
			actions.addComponent(actionButton);
			actions.setComponentAlignment(actionButton, Alignment.TOP_LEFT);
		}

		return actions;
	}

}