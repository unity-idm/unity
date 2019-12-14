/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SingleAuthnConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponentBase;

/**
 * 
 * @author P.Piernik
 *
 */
public class SingleAuthnColumnComponent extends ColumnComponentBase
{
	private AuthenticatorSupportService authenticatorSupport;
	private Supplier<List<String>> authnOptionSupplier;

	private ComboBox<AuthenticationOptionKey> valueComboField;
	private Binder<AuthnOptionKeyBindingValue> binder;

	public SingleAuthnColumnComponent(UnityMessageSource msg, AuthenticatorSupportService authenticatorSupport,
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
		valueComboField = new ComboBox<AuthenticationOptionKey>();
		valueComboField.setItemCaptionGenerator(i -> i.toGlobalKey());
		valueComboField.setWidth(20, Unit.EM);
		List<AuthenticationOptionKey> items = refreshItems();

		binder.forField(valueComboField).withValidator((v, c) -> {
			if (v == null || !authnOptionSupplier.get().contains(v.getAuthenticatorKey()))
			{
				return ValidationResult
						.error(msg.getMessage("SingleAuthnColumnElement.invalidAuthnOption"));
			}
			return ValidationResult.ok();
		}).bind("value");

		binder.setBean(new AuthnOptionKeyBindingValue(items.size() > 0 ? items.get(0) : null));

		return valueComboField;
	}

	private List<AuthenticationOptionKey> refreshItems()
	{
		List<AuthenticationOptionKey> items = new ArrayList<>();
		try
		{
			items.addAll(AuthnColumnComponentHelper.getAvailableAuthnOptions(authenticatorSupport,
					authnOptionSupplier.get(), false));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("SingleAuthnColumnElement.cannotGetItems"), e);
		}
		valueComboField.setItems(items);
		return items;
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

		if (!option.contains("."))
		{
			option += "." + AuthenticationOptionKey.ALL_OPTS;
		}

		AuthenticationOptionKey key = AuthenticationOptionKey.valueOf(option);
		valueComboField.setValue(key);
	}

	@Override
	public SingleAuthnConfig getConfigState()
	{
		return new SingleAuthnConfig(valueComboField.getValue() != null ? valueComboField.getValue().toGlobalKey() : null);
	}

	@Override
	public void addValueChangeListener(Runnable valueChange)
	{
		valueComboField.addValueChangeListener(e -> valueChange.run());

	}

	public static class AuthnOptionKeyBindingValue
	{
		private AuthenticationOptionKey value;

		public AuthnOptionKeyBindingValue(AuthenticationOptionKey value)
		{
			this.value = value;
		}

		public AuthenticationOptionKey getValue()
		{
			return value;
		}

		public void setValue(AuthenticationOptionKey value)
		{
			this.value = value;
		}
	}
}