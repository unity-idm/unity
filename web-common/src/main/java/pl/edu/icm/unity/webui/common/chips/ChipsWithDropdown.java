/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.chips;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.server.ErrorMessage;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.VerticalLayout;

/**
 * In a top row displays a {@link ChipsRow}. Under it a dropdown is displayed. Entry selected in dropdown
 * is added to chips. 
 * @author K. Benedyczak
 */
public class ChipsWithDropdown<T> extends CustomField<List<T>>
{
	private ChipsRow<T> chipsRow;
	protected ComboBox<T> combo;
	private Function<T, String> comboRenderer;
	private Function<T, String> chipRenderer;
	private boolean multiSelectable;
	private Set<T> allItems = new LinkedHashSet<>();
	private boolean readOnly;
	private int maxSelection = 0;
	private VerticalLayout main;
	private final boolean chipsOnTop;
	private boolean skipRemoveInvalidSelections = false;
	private Label emptyComboLabel;
	public ChipsWithDropdown()
	{
		this(Object::toString, true);
	}
	
	public ChipsWithDropdown(Function<T, String> comboRenderer, boolean multiSelectable)
	{
		this(comboRenderer, comboRenderer, multiSelectable, true);
	}

	public ChipsWithDropdown(Function<T, String> comboRenderer, Function<T, String> chipRenderer, boolean multiSelectable)
	{
		this(comboRenderer, chipRenderer, multiSelectable, true);
	}

	public ChipsWithDropdown(Function<T, String> comboRenderer, Function<T, String> chipRenderer, boolean multiSelectable, boolean chipsOnTop)
	{
		this.comboRenderer = comboRenderer;
		this.chipRenderer = chipRenderer;
		this.multiSelectable = multiSelectable;
		chipsRow = new ChipsRow<>();
		chipsRow.addChipRemovalListener(e -> fireEvent(new ValueChangeEvent<List<T>>(this, getSelectedItems(), true)));
		chipsRow.addChipRemovalListener(this::onChipRemoval);
		chipsRow.setVisible(false);
		
		combo = new ComboBox<>();
		combo.setItemCaptionGenerator(item -> comboRenderer.apply(item));
		combo.addSelectionListener(this::onSelectionChange);
		combo.addSelectionListener(e -> fireEvent(new ValueChangeEvent<List<T>>(this, getSelectedItems(), true)));
		emptyComboLabel = new Label();
		emptyComboLabel.setVisible(false);
		
		HorizontalLayout comboWrapper = new HorizontalLayout();
		comboWrapper.setMargin(false);
		comboWrapper.setSpacing(false);
		comboWrapper.addComponent(combo);
		comboWrapper.addComponent(emptyComboLabel);
		comboWrapper.setComponentAlignment(emptyComboLabel, Alignment.MIDDLE_LEFT);
		
		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		this.chipsOnTop = chipsOnTop;
		if (chipsOnTop)
		{
			main.addComponents(chipsRow, comboWrapper);
		} else
		{
			main.addComponents(comboWrapper, chipsRow);
		}
	}
	
	@Override
	protected Component initContent()
	{
		return main;
	}
	
	public void updateComboRenderer(Function<T, String> comboRenderer)
	{
		this.comboRenderer = comboRenderer;
		this.combo.setItemCaptionGenerator(item -> comboRenderer.apply(item));
	}
	
	public void setComboStyleGenerator(StyleGenerator<T> itemStyleGenerator)
	{
		this.combo.setStyleGenerator(itemStyleGenerator);
	}
	
	public void addChipRemovalListener(ClickListener listner)
	{
		chipsRow.addChipRemovalListener(listner);
	}
	
	public void addSelectionListener(SingleSelectionListener<T> listener)
	{
		combo.addSelectionListener(listener);
	}
	
	public void setMultiSelectable(boolean multiSelectable)
	{
		this.multiSelectable = multiSelectable;
		updateItemsAvailableToSelect();
	}
	
	public void setItems(Collection<T> items)
	{
		allItems = new LinkedHashSet<>(items);
		updateItemsAvailableToSelect();
	}
	
	public Set<T> getAllItems()
	{
		return allItems;
	}
	
	public List<T> getAllItemsSorted()
	{
		List<T> items = new ArrayList<>();
		items.addAll(allItems);
		sortItems(items);
		return items;
	}
	
	
	public void setSelectedItems(List<T> items)
	{
		chipsRow.removeAll();
		if (!multiSelectable && items.size() > 1)
			throw new IllegalArgumentException(
					"Can not select more then one element in single-selectable chips");
		if (items != null)
		{
			items.forEach(this::selectItem);
		}
		updateItemsAvailableToSelect();
		verifySelectionLimit();
		chipsRow.setVisible(!(items == null || items.isEmpty()));
	}
	
	@Override
	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		combo.setVisible(!readOnly);
		chipsRow.setReadOnly(readOnly);
	}
	
	public List<T> getSelectedItems()
	{
		return chipsRow.getChipsData();
	}
	
	private void onSelectionChange(SingleSelectionEvent<T> event)
	{
		Optional<T> selectedItem = combo.getSelectedItem();
		if (!selectedItem.isPresent())
			return;
		combo.setSelectedItem(null);
		selectItem(selectedItem.get());
		verifySelectionLimit();
	}

	protected void selectItem(T selected)
	{
		chipsRow.addChip(new Chip<>(chipRenderer.apply(selected), selected));
		chipsRow.setVisible(true);
		updateItemsAvailableToSelect();
	}

	private void updateItemsAvailableToSelect()
	{
		Set<T> selected = new HashSet<>(chipsRow.getChipsData());
		
		//remove not available which were previously selected
		if (combo.getNewItemProvider() == null && !skipRemoveInvalidSelections)
			selected.stream().filter(item -> !allItems.contains(item))
					.forEach(item -> chipsRow.removeItem(item));
		
		List<T> available = checkAvailableItems(allItems, selected);
		
		sortItems(available);
		
		combo.setItems(available);
		if (chipsOnTop)
		{
			if (selected.isEmpty())
			{
				combo.removeStyleName("u-chipsCombo");
			} else
			{
				combo.addStyleName("u-chipsCombo");
			}
		}
		updateComboVisibility(selected, available);
	}
	
	public void setSkipRemoveInvalidSelections(boolean skipRemoveInvalidSelections)
	{
		this.skipRemoveInvalidSelections = skipRemoveInvalidSelections;
	}

	protected void sortItems(List<T> items)
	{
		Collections.sort(items, this::compareItems);
	}
	
	private int compareItems(T a, T b)
	{
		String aStr = comboRenderer.apply(a);
		String bStr = comboRenderer.apply(b);
		return aStr.compareTo(bStr);
	}
	
	protected List<T> checkAvailableItems(Set<T> allItems, Set<T> selected)
	{
		return allItems.stream()
		.filter(i -> !selected.contains(i))
		.collect(Collectors.toList());
	}
		
	protected void updateComboVisibility(Set<T> selected, List<T> available)
	{
		if (!readOnly)
		{
			if (multiSelectable)
			{
				combo.setVisible(!available.isEmpty());
			}
			else
			{
				combo.setVisible(selected.isEmpty() && !available.isEmpty());	
			}
			if (emptyComboLabel.getCaption() != null)
			{
				emptyComboLabel.setVisible(selected.isEmpty() && available.isEmpty());
			}
		}
	}
	
	private void onChipRemoval(ClickEvent event)
	{
		updateItemsAvailableToSelect();
		chipsRow.setVisible(!chipsRow.getChipsData().isEmpty());
		verifySelectionLimit();
	}
	
	private void verifySelectionLimit()
	{
		if (maxSelection > 0)
			combo.setVisible(getSelectedItems().size() < maxSelection);
	}
	
	@Override
	public void setWidth(float width, Unit unit)
	{
		super.setWidth(width, unit);
		if (combo != null)
		{
			combo.setWidth(width, unit);
		}
	}
	
	public void setMaxSelection(int maxSelection)
	{
		this.maxSelection = maxSelection;
		verifySelectionLimit();
	}

	@Override
	public List<T> getValue()
	{
		return getSelectedItems();
	}

	@Override
	protected void doSetValue(List<T> value)
	{
		setSelectedItems(value);
		
	}

	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		super.setComponentError(componentError);
		if (componentError != null)
			combo.addStyleName("error");
		else
			combo.removeStyleName("error");
	}

	public void setEmptyComboLabel(String emptySelectionLabel)
	{
		this.emptyComboLabel.setCaption(emptySelectionLabel);
	}
}
