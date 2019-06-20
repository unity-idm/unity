/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.chips;

import java.util.List;

import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * In a top row displays a {@link ChipsRow}. Under it a textfield with add
 * button is displayed. Entry entered in textfield is added to chips.
 * 
 * @author P.Piernik
 *
 */
public class ChipsWithTextField extends CustomField<List<String>>
{
	private ChipsRow<String> chipsRow;
	private VerticalLayout main;
	private TextField textField;

	@Override
	public List<String> getValue()
	{

		return chipsRow.getChipsData();

	}

	public ChipsWithTextField()
	{
		chipsRow = new ChipsRow<>();
		chipsRow.addChipRemovalListener(e -> fireEvent(
				new ValueChangeEvent<List<String>>(this, chipsRow.getChipsData(), true)));
		chipsRow.addChipRemovalListener(this::onChipRemoval);
		chipsRow.setVisible(false);

		textField = new TextField();
		textField.addShortcutListener(new ShortcutListener("", ShortcutAction.KeyCode.ENTER, null)
		{
			@Override
			public void handleAction(Object sender, Object target)
			{
				addValue();
			}
		});
		
		Button add = new Button();
		add.setIcon(Images.add.getResource());
		add.setStyleName(Styles.vButtonSmall.toString());
		add.addClickListener(e -> addValue());
		add.setVisible(false);
		
		textField.addValueChangeListener(e -> {add.setVisible(e.getValue() != null && !e.getValue().isEmpty());});
			
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.setSpacing(false);
		wrapper.addComponents(textField, add);

		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponents(chipsRow, wrapper);
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	private void addValue()
	{
		String value = textField.getValue();
		if (value != null && !value.isEmpty())
		{
			chipsRow.addChip(new Chip<String>(value, value));
		}
		
		textField.setValue("");
		chipsRow.setVisible(true);
		fireEvent(new ValueChangeEvent<List<String>>(this, chipsRow.getChipsData(), true));
	
	}

	private void onChipRemoval(ClickEvent event)
	{
		chipsRow.setVisible(!chipsRow.getChipsData().isEmpty());
	}

	@Override
	protected void doSetValue(List<String> items)
	{
		chipsRow.removeAll();
		if (items != null)
		{
			items.forEach(s -> chipsRow.addChip(new Chip<String>(s, s)));
		}

		chipsRow.setVisible(!(items == null || items.isEmpty()));
	}

}
