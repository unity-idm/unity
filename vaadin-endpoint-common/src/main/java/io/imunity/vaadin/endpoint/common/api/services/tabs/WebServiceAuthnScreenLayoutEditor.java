/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.AuthnLayoutConfigToUIConverter;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.AuthnLayoutConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.AuthenticationLayoutContent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.AuthnLayoutColumn;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.PaletteButton;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.AuthenticationOptionsSelectorProvider;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.AuthnLayoutComponentsFactory;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.GridAuthnColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.HeaderColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.LastUsedOptionColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.RegistrationColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.SeparatorColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.SingleAuthnColumnComponent;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Authentication screen layout editor
 * 
 * @author P.Piernik
 *
 */
public class WebServiceAuthnScreenLayoutEditor extends CustomField<AuthnLayoutConfiguration>
{
	private final MessageSource msg;
	private final AuthenticationOptionsSelectorProvider authenticationOptionsSelectorProvider;
	private final Supplier<Set<String>> authnOptionSupplier;
	private final Runnable valueChange = () -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), true));

	private List<AuthnLayoutColumn> columns;
	private HorizontalLayout columnsLayout;
	private HorizontalLayout separatorsLayout;
	private List<LocalizedTextFieldDetails> separators;
	private final Runnable dragStart = () -> dragElementStart();
	private final Runnable dragStop = () -> dragElementStop();
	private final Consumer<AuthnLayoutColumn> removeListener = c -> removeColumn(c);
	private final Consumer<ColumnComponent> removeElementListener = e -> removeElementFromColumns(e);
	private VerticalLayout main;
	private Button addColumnButton;
	private PaletteButton regPaletteButton;
	private PaletteButton lastUsedOptionPaletteButton;
	private boolean addColumnVisiable = true;

	public WebServiceAuthnScreenLayoutEditor(MessageSource msg, AuthenticatorSupportService authenticatorSupportService,
			Supplier<Set<String>> authnOptionSupplier)
	{
		this.msg = msg;
		this.authenticationOptionsSelectorProvider = new AuthenticationOptionsSelectorProvider(
				authenticatorSupportService);
		this.authnOptionSupplier = authnOptionSupplier;
		this.columns = new ArrayList<>();
		this.separators = new ArrayList<>();

		initUI();
	}

	private void initUI()
	{
		main = new VerticalLayout();
		main.setMargin(false);
		main.setPadding(false);
		main.setWidthFull();

		main.add(new NativeLabel(msg.getMessage("WebServiceAuthnScreenLayoutEditor.dragElement")));
		main.add(getPallete());
		Div bar = new Div();
		bar.addClassName("u-horizontalBar");
		main.add(bar);

		addColumnButton = new Button();
		addColumnButton.setIcon(new Icon(VaadinIcon.PLUS_CIRCLE_O));
		addColumnButton.setText(msg.getMessage("WebServiceAuthnScreenLayoutEditor.addColumn"));
		addColumnButton.addClickListener(ev -> {
			columns.add(new AuthnLayoutColumn(msg, e -> removeColumn(e), e -> removeElementFromColumns(e),
					valueChange));
			if (columns.size() > 1)
			{
				LocalizedTextFieldDetails field = new LocalizedTextFieldDetails(msg.getEnabledLocales()
						.values(), msg.getLocale());
				field.setPlaceholder(msg.getMessage("WebServiceAuthnScreenLayoutEditor.separator"));
				field.addValueChangeListener(e -> valueChange.run());
				field.setWidth(17, Unit.EM);

				separators.add(field);
			}
			refreshColumns();
			refreshSeparators();
		});

		separatorsLayout = new HorizontalLayout();
		separatorsLayout.setWidthFull();
		separatorsLayout.setSpacing(false);
		separatorsLayout.setMargin(false);
		separatorsLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
		refreshSeparators();

		columnsLayout = new HorizontalLayout();
		columnsLayout.setWidthFull();
		refreshColumns();

		main.add(separatorsLayout);
		main.add(columnsLayout);
		add(main);
		setWidthFull();
	}

	private HorizontalLayout getPallete()
	{
		HorizontalLayout componentsPalette = new HorizontalLayout();
		componentsPalette.add(
				new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.singleAuthn"), VaadinIcon.SIGN_IN, dragStart,
						dragStop, () -> new SingleAuthnColumnComponent(msg, authenticationOptionsSelectorProvider,
								authnOptionSupplier, removeElementListener, valueChange, dragStart, dragStop)));

		componentsPalette.add(new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.gridAuthn"), VaadinIcon.GRID_V,
				dragStart, dragStop, () -> new GridAuthnColumnComponent(msg, authenticationOptionsSelectorProvider,
						authnOptionSupplier, removeElementListener, valueChange, dragStart, dragStop)));

		componentsPalette.add(new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.separator"),
				VaadinIcon.TEXT_LABEL, dragStart, dragStop,
				() -> new SeparatorColumnComponent(msg, removeElementListener, valueChange, dragStart, dragStop)));

		componentsPalette.add(new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.header"), VaadinIcon.HEADER,
				dragStart, dragStop,
				() -> new HeaderColumnComponent(msg, removeElementListener, valueChange, dragStart, dragStop)));

		regPaletteButton = new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.registration"),
				VaadinIcon.USER_CARD, dragStart, dragStop,
				() -> new RegistrationColumnComponent(msg, removeElementListener, dragStart, dragStop));

		componentsPalette.add(regPaletteButton);

		lastUsedOptionPaletteButton = new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.lastUsedOption"),
				VaadinIcon.STAR, dragStart, dragStop,
				() -> new LastUsedOptionColumnComponent(msg, removeElementListener, dragStart, dragStop));

		componentsPalette.add(lastUsedOptionPaletteButton);

		return componentsPalette;
	}

	public void setRegistrationEnabled(boolean enabled)
	{
		regPaletteButton.setVisible(enabled);
	}

	public void setLastUsedOptionEnabled(boolean enabled)
	{
		lastUsedOptionPaletteButton.setVisible(enabled);
	}

	public void setAddColumnEnabled(boolean enabled)
	{
		addColumnButton.setVisible(enabled);
		addColumnVisiable = enabled;
	}

	private void dragElementStart()
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.dragOn();
		}
	}

	private void dragElementStop()
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.dragOff();
		}
		valueChange.run();
	}

	private void refreshSeparators()
	{
		separatorsLayout.removeAll();
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setJustifyContentMode(JustifyContentMode.BETWEEN);
		wrapper.setWidthFull();
		separatorsLayout.add(wrapper);

		HorizontalLayout sepWrapper = null;
		for (LocalizedTextFieldDetails s : separators)
		{
			sepWrapper = new HorizontalLayout();
			sepWrapper.setWidthFull();
			sepWrapper.add(s);
			s.addClassName("u-marginLeftMinus7");
			sepWrapper.setJustifyContentMode(JustifyContentMode.BETWEEN);
			separatorsLayout.add(sepWrapper);
		}

		if (sepWrapper == null)
		{
			wrapper.add(new HorizontalLayout(), addColumnButton);

		} else
		{
			sepWrapper.add(addColumnButton);
		}

	}

	private void refreshColumns()
	{
		columnsLayout.removeAll();
		columnsLayout.setWidthFull();

		for (AuthnLayoutColumn c : columns)
		{
			c.setRemoveVisible(true);
			columnsLayout.add(c);
		}

		if (columns.size() == 1)
		{
			columns.get(0)
					.setRemoveVisible(false);
			columnsLayout.setWidth(50, Unit.PERCENTAGE);
		}

		addColumnButton.setVisible(addColumnVisiable && columns.size() < 4);

		//main.setStyleName("u-minWidth" + columns.size() * 25);

	}

	private void removeElementFromColumns(ColumnComponent e)
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.removeElement(e);
		}
		valueChange.run();
	}

	private void removeColumn(AuthnLayoutColumn c)
	{
		if (!separators.isEmpty())
		{
			if (columns.size() > 1 && columns.indexOf(c) == columns.size() - 1)
			{

				separators.remove(columns.indexOf(c) - 1);

			} else
			{
				separators.remove(columns.indexOf(c));
			}
		}
		columns.remove(c);

		refreshColumns();
		refreshSeparators();
		valueChange.run();
	}

	public void refreshColumnsElements()
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.refreshElements();
		}
	}

	public void validateConfiguration() throws FormValidationException
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.validateConfiguration();
		}
	}

	public void configureBinding(Binder<?> binder, String field)
	{
		binder.forField(this)
				.withValidator((v, c) ->
				{
					try
					{
						validateConfiguration();
					} catch (FormValidationException e)
					{
						return ValidationResult.error("");
					}

					return ValidationResult.ok();

				})
				.bind(field);
	}

	@Override
	public AuthnLayoutConfiguration getValue()
	{
		return AuthnLayoutConfigToUIConverter.convertFromUI(new AuthenticationLayoutContent(columns, separators));
	}

	@Override
	protected AuthnLayoutConfiguration generateModelValue()
	{
		return AuthnLayoutConfigToUIConverter.convertFromUI(new AuthenticationLayoutContent(columns, separators));
	}

	@Override
	protected void setPresentationValue(AuthnLayoutConfiguration newPresentationValue)
	{
		AuthenticationLayoutContent content = AuthnLayoutConfigToUIConverter.convertToUI(newPresentationValue,
				new AuthnLayoutComponentsFactory(msg, removeListener, removeElementListener, dragStart, dragStop,
						valueChange, authenticationOptionsSelectorProvider, authnOptionSupplier, false));
		columns.clear();
		columns.addAll(content.columns);

		separators.clear();
		separators.addAll(content.separators);

		refreshColumns();
		refreshSeparators();

	}
}
