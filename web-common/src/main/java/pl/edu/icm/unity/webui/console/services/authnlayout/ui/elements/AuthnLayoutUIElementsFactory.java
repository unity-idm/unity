/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.AuthnLayoutColumn;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnElement;

/**
 * Factory for auhtentication column UI elements
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutUIElementsFactory
{
	private UnityMessageSource msg;
	private Consumer<AuthnLayoutColumn> removeListener;
	private Consumer<ColumnElement> removeElementListener;
	private Runnable dragStart;
	private Runnable dragStop;
	private Runnable valueChange;
	private AuthenticatorSupportService authenticatorSupportService;
	private Supplier<List<String>> authnOptionSupplier;
	private boolean blockRemoveLastAndExpand;

	public AuthnLayoutUIElementsFactory(UnityMessageSource msg, Consumer<AuthnLayoutColumn> removeListener,
			Consumer<ColumnElement> removeElementListener, Runnable dragStart, Runnable dragStop,
			Runnable valueChange, AuthenticatorSupportService authenticatorSupportService,
			Supplier<List<String>> authnOptionSupplier, boolean blockRemoveLastAndExpand)
	{
		this.msg = msg;
		this.removeListener = removeListener;
		this.removeElementListener = removeElementListener;
		this.dragStart = dragStart;
		this.dragStop = dragStop;
		this.valueChange = valueChange;
		this.authenticatorSupportService = authenticatorSupportService;
		this.authnOptionSupplier = authnOptionSupplier;
		this.blockRemoveLastAndExpand = blockRemoveLastAndExpand;
	}

	public HeaderColumnElement getHeader()
	{
		return new HeaderColumnElement(msg, removeElementListener, valueChange, dragStart, dragStop);

	}

	public ExpandColumnElement getExpand()
	{
		return new ExpandColumnElement(msg, blockRemoveLastAndExpand ? null : removeElementListener, dragStart,
				dragStop);

	}

	public LastUsedOptionColumnElement getLastUsed()
	{
		return new LastUsedOptionColumnElement(msg, blockRemoveLastAndExpand ? null : removeElementListener,
				dragStart, dragStop);
	}

	public SingleAuthnColumnElement getSingleAuthn()
	{
		return new SingleAuthnColumnElement(msg, authenticatorSupportService, authnOptionSupplier,
				removeElementListener, valueChange, dragStart, dragStop);
	}

	public RegistrationColumnElement getRegistration()
	{
		return new RegistrationColumnElement(msg, removeElementListener, dragStart, dragStop);
	}

	public SeparatorColumnElement getSeparator()
	{
		return new SeparatorColumnElement(msg, removeElementListener, valueChange, dragStart, dragStop);
	}

	public GridAuthnColumnElement getGrid()
	{
		return new GridAuthnColumnElement(msg, authenticatorSupportService, authnOptionSupplier,
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
}
