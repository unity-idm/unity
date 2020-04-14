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

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.GridConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponentBase;

/**
 * 
 * @author P.Piernik
 *
 */
public class GridAuthnColumnComponent extends ColumnComponentBase
{
	private AuthenticatorSupportService authenticatorSupport;
	private Supplier<List<String>> authnOptionSupplier;

	private ChipsWithDropdown<AuthenticationOptionKey> valueComboField;
	private Binder<GridStateBindingValue> binder;

	public GridAuthnColumnComponent(MessageSource msg, AuthenticatorSupportService authenticatorSupport,
			Supplier<List<String>> authnOptionSupplier, Consumer<ColumnComponent> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{

		super(msg, msg.getMessage("AuthnColumnLayoutElement.gridAuthn"), Images.grid_v, dragStart, dragStop,
				removeElementListener);
		this.authenticatorSupport = authenticatorSupport;
		this.authnOptionSupplier = authnOptionSupplier;

		addContent(getContent());
		addValueChangeListener(valueChangeListener);
	}

	private Component getContent()
	{
		binder = new Binder<>(GridStateBindingValue.class);
		valueComboField = new ChipsWithDropdown<AuthenticationOptionKey>(i -> i.toGlobalKey(), true);
		valueComboField.setSkipRemoveInvalidSelections(true);
		valueComboField.setWidth(20, Unit.EM);
		List<AuthenticationOptionKey> items = refreshItems();

		binder.forField(valueComboField).withValidator((v, c) -> {
			Set<String> keys = v != null
					? v.stream().map(k -> k.getAuthenticatorKey()).collect(Collectors.toSet())
					: new HashSet<>();
			if (v == null || !authnOptionSupplier.get().containsAll(keys))
			{
				return ValidationResult
						.error(msg.getMessage("GridAuthnColumnElement.invalidAuthnOption"));
			}
			return ValidationResult.ok();
		}).bind("value");

		FormLayout wrapper = new FormLayout();
		wrapper.setMargin(false);
		IntStepper rows = new IntStepper();
		rows.setWidth(3, Unit.EM);
		rows.setCaption(msg.getMessage("GridAuthnColumnElement.rows"));
		wrapper.addComponent(rows);
		rows.setMinValue(1);
		binder.forField(rows).bind("rows");

		binder.setBean(new GridStateBindingValue(
				items.size() > 0 ? Arrays.asList(items.get(0)) : Arrays.asList(), 5));
		VerticalLayout main = new VerticalLayout();
		main.setWidth(20, Unit.EM);
		main.setMargin(false);
		main.addComponents(valueComboField, wrapper);
		
		return main;
	}

	private List<AuthenticationOptionKey> refreshItems()
	{
		List<AuthenticationOptionKey> items = new ArrayList<>();
		try
		{
			items.addAll(AuthnColumnComponentHelper.getAvailableAuthnOptions(authenticatorSupport, authnOptionSupplier.get(), true));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("GridAuthnColumnElement.cannotGetItems"), e);
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
		if (state == null)
			return;

		GridConfig gstate = (GridConfig) state;
		
		List<AuthenticationOptionKey> vals = new ArrayList<>();
		for (String s : gstate.content.split(" "))
		{
			String avs = s;
			if (!s.contains("."))
			{
				avs += "." + AuthenticationOptionKey.ALL_OPTS;
			}
			AuthenticationOptionKey key = AuthenticationOptionKey.valueOf(avs);
			vals.add(key);

		}

		GridStateBindingValue bean = new GridStateBindingValue(vals, gstate.rows);
		binder.setBean(bean);
	}

	@Override
	public AuthnElementConfiguration getConfigState()
	{
		GridStateBindingValue value = binder.getBean();

		return new GridConfig(String.join(" ",
				value.getValue().stream().map(i -> i.toGlobalKey()).collect(Collectors.toList())),
				value.getRows());
	}
	
	@Override
	public void addValueChangeListener(Runnable valueChange)
	{
		valueComboField.addValueChangeListener(e -> valueChange.run());
		
	}

	public static class GridStateBindingValue
	{
		private List<AuthenticationOptionKey> value;
		private int rows;

		public GridStateBindingValue(List<AuthenticationOptionKey> value, int rows)
		{
			this.value = value;
			this.rows = rows;
		}

		public List<AuthenticationOptionKey> getValue()
		{
			return value;
		}

		public void setValue(List<AuthenticationOptionKey> value)
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
	}
}