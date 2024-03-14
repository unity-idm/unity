/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.ui.components;

import com.google.common.base.Objects;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.auth.services.layout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.auth.services.layout.configuration.elements.SingleAuthnConfig;
import io.imunity.vaadin.auth.services.layout.ui.ColumnComponent;
import io.imunity.vaadin.auth.services.layout.ui.ColumnComponentBase;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector.AuthenticationOptionsSelectorComparator;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

public class SingleAuthnColumnComponent extends ColumnComponentBase
{
	private final AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider;
	private Supplier<Set<String>> authnOptionSupplier;

	private ComboBox<AuthenticationOptionsSelector> valueComboField;
	private Binder<AuthnOptionKeyBindingValue> binder;
	private List<AuthenticationOptionsSelector> items;

	public SingleAuthnColumnComponent(MessageSource msg, AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider,
			Supplier<Set<String>> authnOptionSupplier, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.singleAuthn"), VaadinIcon.SIGN_IN, dragStart, dragStop,
				removeElementListener);
		this.authenticationOptionsSelectorProvider = authenticationOptionsSelectorProvider;
		this.authnOptionSupplier = authnOptionSupplier;
		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	private Component getContent()
	{
		binder = new Binder<>(AuthnOptionKeyBindingValue.class);
		valueComboField = new ComboBox<>();
		valueComboField.setItemLabelGenerator(i -> i.getRepresentationFallbackToConfigKey(msg));
		valueComboField.setWidth(TEXT_FIELD_MEDIUM.value());
		refreshItems();
		binder.forField(valueComboField).withValidator((v, c) -> 
		{
			return (v == null || !optionPresent(v)) ? 
				ValidationResult.error(msg.getMessage("SingleAuthnColumnElement.invalidAuthnOption"))
				: ValidationResult.ok();
		}).bind("value");

		binder.setBean(new AuthnOptionKeyBindingValue(items.size() > 0 ? items.get(0) : null));

		return valueComboField;
	}

	private boolean optionPresent(AuthenticationOptionsSelector option)
	{
		return items.stream()
				.filter(key -> key.equals(option))
				.findAny().isPresent();
	}
	
	private void refreshItems()
	{
		items = authenticationOptionsSelectorProvider.getSinglePickerCompatibleAuthnSelectors(authnOptionSupplier.get());
		items.sort(new AuthenticationOptionsSelectorComparator(msg));
		AuthenticationOptionsSelector value = valueComboField.getValue();
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
		String option = ((SingleAuthnConfig) state).authnOption;

		if (option == null || option.isEmpty())
			return;

		valueComboField.setValue(AuthenticationOptionsSelector.valueOf(option));
	}

	@Override
	public SingleAuthnConfig getConfigState()
	{
		return new SingleAuthnConfig(valueComboField.getValue() != null ? valueComboField.getValue().toStringEncodedSelector() : null);
	}

	@Override
	public void addValueChangeListener(Runnable valueChange)
	{
		valueComboField.addValueChangeListener(e -> valueChange.run());
	}

	public static class AuthnOptionKeyBindingValue
	{
		private AuthenticationOptionsSelector value;

		public AuthnOptionKeyBindingValue(AuthenticationOptionsSelector value)
		{
			this.value = value;
		}

		public AuthenticationOptionsSelector getValue()
		{
			return value;
		}

		public void setValue(AuthenticationOptionsSelector value)
		{
			this.value = value;
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hashCode(value);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (getClass() != obj.getClass())
				return false;
			final AuthnOptionKeyBindingValue other = (AuthnOptionKeyBindingValue) obj;

			return Objects.equal(this.value, other.value);		
		}
	}
}