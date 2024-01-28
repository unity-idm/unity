/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Unit;

import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.ExpandConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.GridConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.HeaderConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.LastUsedConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.RegistrationConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.SeparatorConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.SingleAuthnConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.AuthnLayoutColumn;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;


/**
 * Factory for auhtentication column UI elements
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutComponentsFactory
{
	private final MessageSource msg;
	private final Consumer<AuthnLayoutColumn> removeListener;
	private final Consumer<ColumnComponent> removeElementListener;
	private final Runnable dragStart;
	private final Runnable dragStop;
	private final Runnable valueChange;
	private final AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider;
	private final Supplier<Set<String>> authnOptionSupplier;
	private final boolean blockRemoveLastAndExpand;

	public AuthnLayoutComponentsFactory(MessageSource msg, Consumer<AuthnLayoutColumn> removeListener,
			Consumer<ColumnComponent> removeElementListener, Runnable dragStart, Runnable dragStop,
			Runnable valueChange, AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider,
			Supplier<Set<String>> authnOptionSupplier, boolean blockRemoveLastAndExpand)
	{
		this.msg = msg;
		this.removeListener = removeListener;
		this.removeElementListener = removeElementListener;
		this.dragStart = dragStart;
		this.dragStop = dragStop;
		this.valueChange = valueChange;
		this.authenticationOptionsSelectorProvider = authenticationOptionsSelectorProvider;
		this.authnOptionSupplier = authnOptionSupplier;
		this.blockRemoveLastAndExpand = blockRemoveLastAndExpand;
	}

	public HeaderColumnComponent getHeader()
	{
		return new HeaderColumnComponent(msg, removeElementListener, valueChange, dragStart, dragStop);

	}

	public ExpandColumnComponent getExpand()
	{
		return new ExpandColumnComponent(msg, blockRemoveLastAndExpand ? null : removeElementListener, dragStart,
				dragStop);

	}

	public LastUsedOptionColumnComponent getLastUsed()
	{
		return new LastUsedOptionColumnComponent(msg, blockRemoveLastAndExpand ? null : removeElementListener,
				dragStart, dragStop);
	}

	public SingleAuthnColumnComponent getSingleAuthn()
	{
		return new SingleAuthnColumnComponent(msg, authenticationOptionsSelectorProvider, authnOptionSupplier,
				removeElementListener, valueChange, dragStart, dragStop);
	}

	public RegistrationColumnComponent getRegistration()
	{
		return new RegistrationColumnComponent(msg, removeElementListener, dragStart, dragStop);
	}

	public SeparatorColumnComponent getSeparator()
	{
		return new SeparatorColumnComponent(msg, removeElementListener, valueChange, dragStart, dragStop);
	}

	public GridAuthnColumnComponent getGrid()
	{
		return new GridAuthnColumnComponent(msg, authenticationOptionsSelectorProvider, authnOptionSupplier,
				removeElementListener, valueChange, dragStart, dragStop);
	}

	public AuthnLayoutColumn getColumn()
	{
		return new AuthnLayoutColumn(msg, removeListener, removeElementListener, valueChange);
	}

	public LocalizedTextFieldDetails getSeparatorField(I18nString value)
	{
		LocalizedTextFieldDetails sepField = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		sepField.setPlaceholder(msg.getMessage("WebServiceAuthnScreenLayoutEditor.separator"));
		if (value != null)
		{
			sepField.setValue(value.getLocalizedMap());
		}
		sepField.addValueChangeListener(e -> valueChange.run());
		sepField.setWidth(17, Unit.EM);
		return sepField;
	}

	public Optional<ColumnComponent> getForType(AuthnElementConfiguration config)
	{
		if (config instanceof HeaderConfig)
		{
			return Optional.of(getHeader());

		} else if (config instanceof SeparatorConfig)
		{
			return Optional.of(getSeparator());
		} else if (config instanceof SingleAuthnConfig)
		{
			return Optional.of(getSingleAuthn());
		} else if (config instanceof GridConfig)
		{
			return Optional.of(getGrid());
		} else if (config instanceof ExpandConfig)
		{
			return Optional.of(getExpand());
		} else if (config instanceof LastUsedConfig)
		{
			return Optional.of(getLastUsed());
		} else if (config instanceof RegistrationConfig)
		{
			return Optional.of(getRegistration());
		}
		return Optional.empty();
	}
}
