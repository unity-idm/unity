/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.vaadin.risto.stepper.IntStepper;

import com.google.common.base.Objects;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector.AuthenticationOptionsSelectorComparator;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.GridConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponentBase;

public class GridAuthnColumnComponent extends ColumnComponentBase
{
	private final AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider;
	private Supplier<List<String>> authnOptionSupplier;

	private ChipsWithDropdown<AuthenticationOptionsSelector> valueComboField;
	private Binder<GridStateBindingValue> binder;
	private List<AuthenticationOptionsSelector> items;

	public GridAuthnColumnComponent(MessageSource msg, AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider,
			Supplier<List<String>> authnOptionSupplier, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.gridAuthn"), Images.grid_v, dragStart, dragStop,
				removeElementListener);
		this.authenticationOptionsSelectorProvider = authenticationOptionsSelectorProvider;
		this.authnOptionSupplier = authnOptionSupplier;

		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	private Component getContent()
	{
		binder = new Binder<>(GridStateBindingValue.class);
		valueComboField = new AuthnSelector(msg);
		valueComboField.setSkipRemoveInvalidSelections(true);
		valueComboField.setWidth(20, Unit.EM);
		refreshItems();

		binder.forField(valueComboField).withValidator((v, c) -> 
		{
			return (v == null || !allOptionsPresent(v)) ? 
				ValidationResult.error(msg.getMessage("GridAuthnColumnElement.invalidAuthnOption")) :
				ValidationResult.ok();
		}).bind("value");

		FormLayout wrapper = new FormLayout();
		wrapper.setMargin(false);
		IntStepper rows = new IntStepper();
		rows.setWidth(3, Unit.EM);
		rows.setCaption(msg.getMessage("GridAuthnColumnElement.rows"));
		wrapper.addComponent(rows);
		rows.setMinValue(1);
		binder.forField(rows).bind("rows");

		binder.setBean(new GridStateBindingValue(Arrays.asList(), 5));
		VerticalLayout main = new VerticalLayout();
		main.setWidth(20, Unit.EM);
		main.setMargin(false);
		main.addComponents(valueComboField, wrapper);
		
		return main;
	}

	private boolean allOptionsPresent(List<AuthenticationOptionsSelector> options)
	{
		Set<AuthenticationOptionsSelector> availableSet = new HashSet<>(items);
		return !options.stream()
				.filter(key -> !availableSet.contains(key))
				.findAny().isPresent();
	}
	
	private void refreshItems()
	{
		items = authenticationOptionsSelectorProvider.getGridCompatibleAuthnSelectors(authnOptionSupplier.get());
		valueComboField.setItems(items);
	}

	@Override
	public void refresh()
	{
		refreshItems();
		binder.validate();
	}

	@Override
	public void validate() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException();
		}
	}

	@Override
	public void setConfigState(AuthnElementConfiguration state)
	{
		if (state == null)
			return;

		GridConfig gstate = (GridConfig) state;
		
		List<AuthenticationOptionsSelector> vals = new ArrayList<>();
		for (String avs : gstate.content.split(" "))
			vals.add(AuthenticationOptionsSelector.valueOf(avs));

		GridStateBindingValue bean = new GridStateBindingValue(vals, gstate.rows);
		binder.setBean(bean);
	}

	@Override
	public AuthnElementConfiguration getConfigState()
	{
		GridStateBindingValue value = binder.getBean();

		return new GridConfig(String.join(" ",
				value.getValue().stream().map(i -> i.toStringEncodedSelector()).collect(Collectors.toList())),
				value.getRows());
	}
	
	@Override
	public void addValueChangeListener(Runnable valueChange)
	{
		valueComboField.addValueChangeListener(e -> valueChange.run());
		
	}

	public static class GridStateBindingValue
	{
		private List<AuthenticationOptionsSelector> value;
		private int rows;

		public GridStateBindingValue(List<AuthenticationOptionsSelector> value, int rows)
		{
			this.value = value;
			this.rows = rows;
		}

		public List<AuthenticationOptionsSelector> getValue()
		{
			return value;
		}

		public void setValue(List<AuthenticationOptionsSelector> value)
		{
			this.value = value;
		}

		public int getRows()
		{
			return rows;
		}

		public void setRows(int rows)
		{
			this.rows = rows;
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hashCode(value, rows);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (getClass() != obj.getClass())
				return false;
			final GridStateBindingValue other = (GridStateBindingValue) obj;

			return Objects.equal(this.value, other.value) && Objects.equal(this.rows, other.rows);		
		}
	}
	
	private static class AuthnSelector extends ChipsWithDropdown<AuthenticationOptionsSelector>
	{
		private MessageSource msg;

		private AuthnSelector(MessageSource msg)
		{
			super(i -> i.getRepresentationFallbackToConfigKey(msg), true);
			this.msg = msg;
		}
		
		@Override
		protected void sortItems(List<AuthenticationOptionsSelector> items)
		{
			items.sort(new AuthenticationOptionsSelectorComparator(msg));
		}	
	}
}