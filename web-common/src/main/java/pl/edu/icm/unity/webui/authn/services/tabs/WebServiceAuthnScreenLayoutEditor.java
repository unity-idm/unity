/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.authnlayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.services.authnlayout.AuthnLayoutPropertiesHelper.AuthenticationLayoutContent;
import pl.edu.icm.unity.webui.authn.services.layout.elements.GridAuthnColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.HeaderColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.LastUsedOptionColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.RegistrationColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.SeparatorColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.SingleAuthnColumnElement;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Authentication screen layout editor
 * @author P.Piernik
 *
 */
public class WebServiceAuthnScreenLayoutEditor extends CustomComponent
{
	private UnityMessageSource msg;
	private List<AuthnLayoutColumn> columns;
	private HorizontalLayout columnsLayout;
	private HorizontalLayout separatorsLayout;
	private List<I18nTextField> separators;

	private AuthenticatorSupportService authenticatorSupportService;
	private Supplier<List<String>> authnOptionSupplier;

	private Runnable dragStart = () -> dragElementStart();
	private Runnable dragStop = () -> dragElementStop();
	private Consumer<ColumnElement> removeElementListener = e -> removeElementFromColumns(e);
	private VerticalLayout main;
	private Button addColumnButton;

	public WebServiceAuthnScreenLayoutEditor(VaadinEndpointProperties properties, UnityMessageSource msg,
			AuthenticatorSupportService authenticatorSupportService,
			Supplier<List<String>> authnOptionSupplier)
	{
		this.msg = msg;
		this.authenticatorSupportService = authenticatorSupportService;
		this.authnOptionSupplier = authnOptionSupplier;
		this.columns = new ArrayList<>();
		this.separators = new ArrayList<>();

		initUI();
		Consumer<AuthnLayoutColumn> removeListener = c -> removeColumn(c);
		AuthenticationLayoutContent content = AuthnLayoutPropertiesHelper.loadFromProperties(properties, msg, removeListener,
				removeElementListener, dragStart, dragStop, authenticatorSupportService,
				authnOptionSupplier);
		columns = content.columns;
		separators = content.separators;
		if (columns.isEmpty())
		{
			columns.add(new AuthnLayoutColumn(msg, removeListener, removeElementListener, dragStart, dragStop));
		}

		refreshColumns();
		refreshSeparators();
	}
	
	private void initUI()
	{
	
		Panel panel = new Panel();
		panel.setStyleName(Styles.vPanelBorderless.toString());
			
		main = new VerticalLayout();
		main.setMargin(false);
		main.setWidth(100, Unit.PERCENTAGE);
		
		main.addComponent(new Label(msg.getMessage("WebServiceAuthnScreenLayoutEditor.dragElement")));
		main.addComponent(getPallete());
		main.addComponent(HtmlTag.horizontalLine());

		addColumnButton = new Button();
		addColumnButton.setIcon(Images.add.getResource());
		addColumnButton.setCaption(msg.getMessage("WebServiceAuthnScreenLayoutEditor.addColumn"));
		addColumnButton.addClickListener(ev -> {
			columns.add(new AuthnLayoutColumn(msg, e -> removeColumn(e), e -> removeElementFromColumns(e),
					() -> dragElementStart(), () -> dragElementStop()));
			if (columns.size() > 1)
			{
				separators.add(new I18nTextField(msg));
			}
			refreshColumns();
			refreshSeparators();
		});
		
		separatorsLayout = new HorizontalLayout();
		separatorsLayout.setWidth(100, Unit.PERCENTAGE);
		separatorsLayout.setSpacing(false);
		separatorsLayout.setMargin(false);
		refreshSeparators();

		columnsLayout = new HorizontalLayout();
		columnsLayout.setWidth(100, Unit.PERCENTAGE);
		refreshColumns();

		main.addComponent(separatorsLayout);
		main.addComponent(columnsLayout);
		panel.setContent(main);
		panel.setWidth(100, Unit.PERCENTAGE);;
		
		setCompositionRoot(panel);
	}

	private HorizontalLayout getPallete()
	{
		HorizontalLayout componentsPalette = new HorizontalLayout();
		componentsPalette.addComponent(new PalleteButton(msg.getMessage("AuthnColumnLayoutElement.singleAuthn"),
				Images.sign_in.getResource(), dragStart, dragStop,
				() -> new SingleAuthnColumnElement(msg, authenticatorSupportService,
						authnOptionSupplier, removeElementListener, dragStart, dragStop)));

		componentsPalette.addComponent(new PalleteButton(msg.getMessage("AuthnColumnLayoutElement.gridAuthn"),
				Images.grid_v.getResource(), dragStart, dragStop,
				() -> new GridAuthnColumnElement(msg, authenticatorSupportService, authnOptionSupplier,
						removeElementListener, dragStart, dragStop)));

		componentsPalette.addComponent(new PalleteButton(msg.getMessage("AuthnColumnLayoutElement.separator"),
				Images.text.getResource(), dragStart, dragStop,
				() -> new SeparatorColumnElement(msg, removeElementListener, dragStart, dragStop)));

		componentsPalette.addComponent(new PalleteButton(msg.getMessage("AuthnColumnLayoutElement.header"),
				Images.header.getResource(), dragStart, dragStop,
				() -> new HeaderColumnElement(msg, removeElementListener, dragStart, dragStop)));

		componentsPalette.addComponent(new PalleteButton(
				msg.getMessage("AuthnColumnLayoutElement.registration"),
				Images.addIdentity.getResource(), dragStart, dragStop,
				() -> new RegistrationColumnElement(msg, removeElementListener, dragStart, dragStop)));

		componentsPalette.addComponent(new PalleteButton(
				msg.getMessage("AuthnColumnLayoutElement.lastUsedOption"), Images.star.getResource(),
				dragStart, dragStop, () -> new LastUsedOptionColumnElement(msg, removeElementListener,
						dragStart, dragStop)));

		return componentsPalette;
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
	}


	private void refreshSeparators()
	{
		separatorsLayout.removeAllComponents();
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setWidth(100, Unit.PERCENTAGE);
		separatorsLayout.addComponent(wrapper);

		HorizontalLayout sepWrapper = null;
		for (I18nTextField s : separators)
		{
			sepWrapper = new HorizontalLayout();
			sepWrapper.setWidth(100, Unit.PERCENTAGE);
			sepWrapper.addComponent(s);
			s.addStyleName("u-marginLeftMinus7");
			separatorsLayout.addComponent(sepWrapper);
		}

		if (sepWrapper == null)
		{
			wrapper.addComponent(addColumnButton);
			wrapper.setComponentAlignment(addColumnButton, Alignment.MIDDLE_RIGHT);

		} else
		{
			sepWrapper.addComponent(addColumnButton);
			sepWrapper.setComponentAlignment(addColumnButton, Alignment.MIDDLE_RIGHT);
		}

	}

	private void refreshColumns()
	{
		columnsLayout.removeAllComponents();
		columnsLayout.setWidth(100, Unit.PERCENTAGE);
		
		for (AuthnLayoutColumn c : columns)
		{
			c.setRemoveVisible(true);
			columnsLayout.addComponent(c);
		}

		if (columns.size() == 1)
		{
			columns.get(0).setRemoveVisible(false);
			columnsLayout.setWidth(50, Unit.PERCENTAGE);
		}

		addColumnButton.setVisible(columns.size() < 4);
		
		main.setStyleName("u-minWidth" + columns.size() * 25);

	}

	private void removeElementFromColumns(ColumnElement e)
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.removeElement(e);
		}
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
	}

	public void refreshColumnsElements()
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.refreshElements();
		}
	}

	public Properties getConfiguration() throws FormValidationException
	{
		validateConfiguration();
		return AuthnLayoutPropertiesHelper.toProperties(msg, new AuthenticationLayoutContent(columns, separators));
	}

	public void validateConfiguration() throws FormValidationException
	{
		for (AuthnLayoutColumn c : columns)
		{
			c.validateConfiguration();
		}
	}
}
