/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.ExpandConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.GridConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.HeaderConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.LastUsedConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.RegistrationConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SeparatorConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SingleAuthnConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.AuthnLayoutColumn;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;

/**
 * Factory for auhtentication column UI elements
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutComponentsFactory
{
	private MessageSource msg;
	private Consumer<AuthnLayoutColumn> removeListener;
	private Consumer<ColumnComponent> removeElementListener;
	private Runnable dragStart;
	private Runnable dragStop;
	private Runnable valueChange;
	private AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider;
	private Supplier<List<String>> authnOptionSupplier;
	private boolean blockRemoveLastAndExpand;

	public AuthnLayoutComponentsFactory(MessageSource msg, Consumer<AuthnLayoutColumn> removeListener,
			Consumer<ColumnComponent> removeElementListener, Runnable dragStart, Runnable dragStop,
			Runnable valueChange, AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider,
			Supplier<List<String>> authnOptionSupplier, boolean blockRemoveLastAndExpand)
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

	public I18nTextField getSeparatorField(I18nString value)
	{
		I18nTextField sepField = new I18nTextField(msg);
		sepField.setPlaceholder(msg.getMessage("WebServiceAuthnScreenLayoutEditor.separator"));
		if (value != null)
		{
			sepField.setValue(value);
		}
		sepField.addValueChangeListener(e -> valueChange.run());
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
