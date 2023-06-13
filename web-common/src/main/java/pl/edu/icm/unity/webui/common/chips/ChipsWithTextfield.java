/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.chips;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.binding.SingleStringFieldBinder;

/**
 * In a top row displays a {@link ChipsRow}. Under it a {@link TextField} is
 * displayed. Any text entered in textfield is added to chips.
 *
 * @author K. Benedyczak
 */
public class ChipsWithTextfield extends CustomField<List<String>>
{
	private ChipsRow<String> chipsRow;
	private TextField textInput;
	private boolean readOnly;
	private int maxSelection = 0;
	private VerticalLayout main;
	private SingleStringFieldBinder binder;

	private Registration shortcutReg;

	public ChipsWithTextfield(MessageSource msg)
	{
		this(msg, true, true);
	}

	public ChipsWithTextfield(MessageSource msg, boolean multiSelectable, boolean chipsOnTop)
	{
		this.maxSelection = multiSelectable ? Integer.MAX_VALUE : 1;
		chipsRow = new ChipsRow<>();
		chipsRow.addChipRemovalListener(
				e -> fireEvent(new ValueChangeEvent<List<String>>(this, getItems(), true)));
		chipsRow.addChipRemovalListener(this::onChipRemoval);
		chipsRow.setVisible(false);

		textInput = new TextField();
		textInput.setPlaceholder(msg.getMessage("typeAndConfirm"));
		ShortcutListener shortcutListener = new ShortcutListener("Default key", KeyCode.ENTER, null)
		{
			@Override
			public void handleAction(Object sender, Object target)
			{
				onSelectionChange();
			}
		};

		textInput.addFocusListener(e -> {

			shortcutReg = textInput.addShortcutListener(shortcutListener);
		});

		textInput.addBlurListener(e -> {
			if (shortcutReg != null)
				shortcutReg.remove();
		});

		textInput.setDescription(msg.getMessage("typeAndConfirm"));

		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		if (chipsOnTop)
		{
			main.addComponents(chipsRow, textInput);
		} else
		{
			main.addComponents(textInput, chipsRow);
		}
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	public void addChipRemovalListener(ClickListener listner)
	{
		chipsRow.addChipRemovalListener(listner);
	}

	public void setValidator(MessageSource msg, SerializablePredicate<String> validityPredicate,
			String message)
	{
		binder = new SingleStringFieldBinder(msg);
		binder.forField(textInput).withValidator(validityPredicate, message).bind("value");
	}

	public void setMultiSelectable(boolean multiSelectable)
	{
		this.maxSelection = multiSelectable ? Integer.MAX_VALUE : 1;
		updateTextInputVisibility();
	}

	public void setItems(List<String> items)
	{
		chipsRow.removeAll();
		if (items != null)
		{
			if (items.size() > maxSelection)
				throw new IllegalArgumentException(
						"Can not select more elements in size bound chips, max is "
								+ maxSelection);

			items.forEach(this::selectItem);
		}
		updateTextInputVisibility();
		chipsRow.setVisible(!(items == null || items.isEmpty()));
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		textInput.setVisible(!readOnly);
		chipsRow.setReadOnly(readOnly);
	}

	public List<String> getItems()
	{
		return chipsRow.getChipsData();
	}

	private void onSelectionChange()
	{
		textInput.setComponentError(null);
		String value = textInput.getValue();
		if (value.isEmpty() || (binder != null && !binder.isValid()))
			return;
		textInput.setValue("");
		selectItem(value);
		updateTextInputVisibility();
		fireEvent(new ValueChangeEvent<List<String>>(this, getItems(), true));
	}

	protected void selectItem(String selected)
	{
		chipsRow.addChip(new Chip<>(selected, selected));
		chipsRow.setVisible(true);
		updateTextInputVisibility();
		fireEvent(new ValueChangeEvent<List<String>>(this, getItems(), true));
	}

	protected void sortItems(List<String> items)
	{
		Collections.sort(items, this::compareItems);
	}

	private int compareItems(String a, String b)
	{
		return a.compareTo(b);
	}

	protected List<String> checkAvailableItems(Set<String> allItems, Set<String> selected)
	{
		return allItems.stream().filter(i -> !selected.contains(i)).collect(Collectors.toList());
	}

	private void onChipRemoval(ClickEvent event)
	{
		chipsRow.setVisible(!chipsRow.getChipsData().isEmpty());
		updateTextInputVisibility();
	}

	private void updateTextInputVisibility()
	{
		if (readOnly)
			textInput.setVisible(false);
		else if (maxSelection > 0)
			textInput.setVisible(getItems().size() < maxSelection);
	}

	@Override
	public void setWidth(float width, Unit unit)
	{
		super.setWidth(width, unit);
		if (textInput != null)
			textInput.setWidth(width, unit);
	}

	public void setMaxSelection(int maxSelection)
	{
		this.maxSelection = maxSelection;
		updateTextInputVisibility();
	}

	@Override
	public List<String> getValue()
	{
		return getItems();
	}

	@Override
	protected void doSetValue(List<String> value)
	{
		setItems(value);
	}
}
