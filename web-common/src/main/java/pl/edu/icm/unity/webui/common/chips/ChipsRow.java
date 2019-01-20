/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.chips;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;

/**
 * Row of {@link Chip} components. Chip button click is deleting a chip from the component.
 * 
 * @author K. Benedyczak
 */
public class ChipsRow<T> extends CustomComponent
{
	private List<Chip<T>> chips;
	private CssLayout wrapper;
	private List<ClickListener> externalRemovalListeners;
	private boolean readOnly;
	
	public ChipsRow()
	{
		wrapper = new CssLayout();
		wrapper.setWidth(100, Unit.PERCENTAGE);
		setCompositionRoot(wrapper);
		chips = new ArrayList<>();
	}
	
	public void addChip(Chip<T> chip)
	{
		chips.add(chip);
		chip.setReadOnly(readOnly);
		wrapper.addComponent(chip);
		chip.addRemovalListener(c -> removeChip(chip));
		if (externalRemovalListeners != null)
		{
			for (ClickListener l : externalRemovalListeners)
			chip.addRemovalListener(l);
		}
			
	}
	
	public List<T> getChipsData()
	{
		return chips.stream().map(chip -> chip.getValue()).collect(Collectors.toList());
	}
	
	public void addChipRemovalListener(ClickListener externalRemovalListener)
	{
		if (externalRemovalListeners == null)
			externalRemovalListeners = new ArrayList<>();
		externalRemovalListeners.add(externalRemovalListener);
		for (Chip<T> chip: chips)
			chip.addRemovalListener(externalRemovalListener);
	}
	
	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		for (Chip<T> chip: chips)
			chip.setReadOnly(readOnly);
	}

	public void removeItem(T item)
	{
		for (int i=0; i<chips.size(); i++)
		{
			Chip<T> chip = chips.get(i);
			if (Objects.equals(chip.getValue(), item))
			{
				chips.remove(i);
				wrapper.removeComponent(chip);
				break;
			}
		}
	}
	
	public void removeAll()
	{
		chips.clear();
		wrapper.removeAllComponents();
	}
	
	private void removeChip(Chip<T> chip)
	{
		chips.remove(chip);
		wrapper.removeComponent(chip);
	}
}
