/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.vaadin.ui.ComboBox;

/**
 * Simple {@link ComboBox} which doesn't allow for empty selections, unless there is no contents.
 * 
 * @author K. Benedyczak
 */
public class NotNullComboBox<T> extends ComboBox<T>
{
	public NotNullComboBox(String caption)
	{
		super(caption);
		init();
	}

	protected final void init()
	{
		setEmptySelectionAllowed(false);
	}
	
	public void addItemsWithFirstValueSelected(CaptionFilter captionFilter, List<T> items)
	{
		if (captionFilter != null)
			super.setItems(captionFilter, items);
		else
			super.setItems(items);
		if (items != null && items.size() > 0)
			setSelectedItem(items.get(0));
	}
	
	@Override
	public void setItems(Collection<T> items)
	{
		if (items == null)
			throw new IllegalArgumentException("items must not be null");
		addItemsWithFirstValueSelected(null, Lists.newArrayList(items));
	}

	@Override
	public void setItems(Stream<T> streamOfItems)
	{
		if (streamOfItems == null)
			throw new IllegalArgumentException("streamOfItems must not be null");
		addItemsWithFirstValueSelected(null, Lists.newArrayList(streamOfItems.collect(Collectors.toList())));
	}

	@Override
	public void setItems(@SuppressWarnings("unchecked") T... items)
	{
		if (items == null)
			throw new IllegalArgumentException("items must not be null");
		addItemsWithFirstValueSelected(null, Arrays.asList(items));
	}

	@Override
	public void setItems(CaptionFilter captionFilter, Collection<T> items)
	{
		if (items == null)
			throw new IllegalArgumentException("items must not be null");
		addItemsWithFirstValueSelected(captionFilter, Lists.newArrayList(items));
	}

	@Override
	public void setItems(CaptionFilter captionFilter, @SuppressWarnings("unchecked") T... items)
	{
		if (items == null)
			throw new IllegalArgumentException("items must not be null");
		addItemsWithFirstValueSelected(captionFilter, Arrays.asList(items));
	}
}
