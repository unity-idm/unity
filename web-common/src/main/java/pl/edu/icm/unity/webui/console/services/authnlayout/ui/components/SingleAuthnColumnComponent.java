/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Objects;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SingleAuthnConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponentBase;

public class SingleAuthnColumnComponent extends ColumnComponentBase
{
	private AuthenticatorSupportService authenticatorSupport;
	private Supplier<List<String>> authnOptionSupplier;

	private ComboBox<AuthenticationOptionsSelector> valueComboField;
	private Binder<AuthnOptionKeyBindingValue> binder;
	private List<AuthenticationOptionsSelector> items;

	public SingleAuthnColumnComponent(MessageSource msg, AuthenticatorSupportService authenticatorSupport,
			Supplier<List<String>> authnOptionSupplier, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.singleAuthn"), Images.sign_in, dragStart, dragStop,
				removeElementListener);
		this.authenticatorSupport = authenticatorSupport;
		this.authnOptionSupplier = authnOptionSupplier;
		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	private Component getContent()
	{
		binder = new Binder<>(AuthnOptionKeyBindingValue.class);
		valueComboField = new ComboBox<>();
		valueComboField.setItemCaptionGenerator(i -> i.getDisplayedNameFallbackToConfigKey(msg));
		valueComboField.setWidth(20, Unit.EM);
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
		items = AuthnColumnComponentHelper.getSinglePickerCompatibleAuthnSelectors(
				authenticatorSupport, authnOptionSupplier.get());
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