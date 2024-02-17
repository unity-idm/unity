/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Objects;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.GridConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponentBase;
import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;


public class GridAuthnColumnComponent extends ColumnComponentBase
{
	private final AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider;
	private final Supplier<Set<String>> authnOptionSupplier;

	private MultiSelectComboBox<AuthenticationOptionsSelector> valueComboField;
	private Binder<GridStateBindingValue> binder;
	private List<AuthenticationOptionsSelector> items;

	public GridAuthnColumnComponent(MessageSource msg, AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider,
			Supplier<Set<String>> authnOptionSupplier, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.gridAuthn"), VaadinIcon.GRID_V, dragStart, dragStop,
				removeElementListener);
		this.authenticationOptionsSelectorProvider = authenticationOptionsSelectorProvider;
		this.authnOptionSupplier = authnOptionSupplier;

		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	private Component getContent()
	{
		binder = new Binder<>(GridStateBindingValue.class);
		valueComboField = new MultiSelectComboBox<>();
		valueComboField.setItemLabelGenerator(i -> i.getRepresentationFallbackToConfigKey(msg));
		valueComboField.setWidthFull();
		
		refreshItems();

		binder.forField(valueComboField).withConverter(List::copyOf, HashSet::new).withValidator((v, c) -> 
		{
			return (v == null || !allOptionsPresent(v)) ? 
				ValidationResult.error(msg.getMessage("GridAuthnColumnElement.invalidAuthnOption")) :
				ValidationResult.ok();
		}).bind("value");

		FormLayout wrapper = new FormLayout();
		wrapper.addClassName(CssClassNames.SMALL_VAADIN_FORM_ITEM_LABEL.getName());
		IntegerField rows = new IntegerField();
		rows.setWidth(7, Unit.EM);
		rows.setStepButtonsVisible(true);
		wrapper.addFormItem(rows, msg.getMessage("GridAuthnColumnElement.rows"));
		rows.setMin(1);
		binder.forField(rows).bind("rows");

		binder.setBean(new GridStateBindingValue(Arrays.asList(), 5));
		VerticalLayout main = new VerticalLayout();
		main.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		main.setMargin(false);
		main.setPadding(false);
		main.setSpacing(false);
		main.add(valueComboField, wrapper);
		
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
		Set<AuthenticationOptionsSelector> value = valueComboField.getValue();
		valueComboField.setItems(items);
		valueComboField.setValue(value);
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

	
//	private static class AuthnSelector extends ChipsWithDropdown<AuthenticationOptionsSelector>
//	{
//		private MessageSource msg;
//
//		private AuthnSelector(MessageSource msg)
//		{
//			super(i -> i.getRepresentationFallbackToConfigKey(msg), true);
//			this.msg = msg;
//		}
//		
//		@Override
//		protected void sortItems(List<AuthenticationOptionsSelector> items)
//		{
//			items.sort(new AuthenticationOptionsSelectorComparator(msg));
//		}	
//	}
}