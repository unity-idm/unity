/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.chips;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * In a top row displays a {@link ChipsRow}. Under it a dropdown is displayed. Entry selected in dropdown
 * is added to chips. 
 * @author K. Benedyczak
 */
public class ChipsWithDropdown<T> extends CustomComponent
{
	private ChipsRow<T> chipsRow;
	private ComboBox<T> combo;
	private Function<T, String> renderer;
	private boolean multiSelectable;
	private Set<T> allItems = new LinkedHashSet<>();
	private boolean readOnly;
	private int maxSelection = 0;

	public ChipsWithDropdown()
	{
		this(Object::toString, true);
	}
	
	public ChipsWithDropdown(Function<T, String> renderer, boolean multiSelectable)
	{
		this.renderer = renderer;
		this.multiSelectable = multiSelectable;
		chipsRow = new ChipsRow<>();
		chipsRow.addChipRemovalListener(this::onChipRemoval);
		chipsRow.setVisible(false);
		combo = new ComboBox<>();
		combo.setItemCaptionGenerator(item -> renderer.apply(item));
		combo.addSelectionListener(this::onSelectionChange);
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponents(chipsRow, combo);
		setCompositionRoot(main);
	}
	
	public void setMultiSelectable(boolean multiSelectable)
	{
		this.multiSelectable = multiSelectable;
		updateItemsAvailableToSelect();
	}
	
	public void setItems(List<T> items)
	{
		allItems = new LinkedHashSet<>(items);
		updateItemsAvailableToSelect();
	}
	
	public void setSelectedItems(List<T> items)
	{
		if (!multiSelectable && items.size() > 1)
			throw new IllegalArgumentException("Can not select more then one element in single-selectable chips");
		items.forEach(this::selectGroup);
		updateItemsAvailableToSelect();
		verifySelectionLimit();
		chipsRow.setVisible(!items.isEmpty());
		
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
		selectGroup(selectedItem.get());
		verifySelectionLimit();
	}

	private void selectGroup(T selected)
	{
		chipsRow.addChip(new Chip<>(renderer.apply(selected), selected));
		chipsRow.setVisible(true);
		updateItemsAvailableToSelect();
	}

	private void updateItemsAvailableToSelect()
	{
		Set<T> selected = new HashSet<>(chipsRow.getChipsData());
		
		//remove not available which were previously selected
		selected.stream()
			.filter(item -> !allItems.contains(item))
			.forEach(item -> chipsRow.removeItem(item));
		
		List<T> available = allItems.stream()
				.filter(i -> !selected.contains(i))
				.collect(Collectors.toList());
		
		Collections.sort(available, this::compareItems);
		combo.setItems(available);
		if (selected.isEmpty())
			combo.removeStyleName("u-chipsCombo");
		else
			combo.addStyleName("u-chipsCombo");
		updateComboVisibility(selected, available);
	}
	
	private int compareItems(T a, T b)
	{
		String aStr = renderer.apply(a);
		String bStr = renderer.apply(b);
		return aStr.compareTo(bStr);
	}
	
	private void updateComboVisibility(Set<T> selected, List<T> available)
	{
		if (!readOnly)
		{
			if (multiSelectable)
				combo.setVisible(!available.isEmpty());
			else
				combo.setVisible(selected.isEmpty() && !available.isEmpty());
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
			combo.setEnabled(getSelectedItems().size() < maxSelection);
	}
	
	@Override
	public void setWidth(float width, Unit unit)
	{
		super.setWidth(width, unit);
		if (combo != null)
			combo.setWidth(width, unit);
	}
	
	public void setMaxSelection(int maxSelection)
	{
		this.maxSelection = maxSelection;
		verifySelectionLimit();
	}
}
