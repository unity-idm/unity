/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credentials;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.*;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorFactory;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;

import java.util.Optional;
import java.util.Set;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;


@PermitAll
@Route(value = "/credentials/edit", layout = ConsoleMenu.class)
public class CredentialsEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final CredentialsController credentialsController;
	private final CredentialEditorRegistry credentialEditorReg;
	private final EventsBus bus;

	private Select<LocalCredentialState> newCredState;
	private ComboBox<String> credentialType;
	private Panel credentialEditorPanel;
	private CredentialDefinitionEditor cdEd;
	private boolean edit;
	private BreadCrumbParameter breadCrumbParameter;
	private Binder<CredentialDefinition> binder;

	CredentialsEditView(MessageSource msg, CredentialEditorRegistry credentialEditorReg, CredentialsController credentialsController)
	{
		this.msg = msg;
		this.credentialEditorReg = credentialEditorReg;
		this.credentialsController = credentialsController;
		this.bus = WebSession.getCurrent().getEventBus();

	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String credentialName)
	{
		getContent().removeAll();

		CredentialDefinition definition;
		Set<String> supportedTypes = credentialEditorReg.getSupportedTypes();
		if(credentialName == null)
		{
			definition = new CredentialDefinition();
			definition.setDisplayedName(new I18nString());
			definition.setDescription(new I18nString());
			definition.setTypeId(supportedTypes.stream().findFirst().orElse(""));
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			edit = false;
		}
		else
		{
			definition = credentialsController.getCredential(credentialName);
			breadCrumbParameter = new BreadCrumbParameter(credentialName, credentialName);
			edit = true;
		}
		initUI(definition, supportedTypes);
	}

	private void initUI(CredentialDefinition initial, Set<String> supportedTypes)
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.setClassName(CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL.getName());

		TextField name = new TextField();
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		name.setPlaceholder(msg.getMessage("CredentialDefinition.defaultName"));
		main.addFormItem(name, msg.getMessage("CredentialDefinition.name"));

		LocalizedTextFieldDetails displayedName = new LocalizedTextFieldDetails(
				msg.getEnabledLocales().values(), msg.getLocale());
		displayedName.setWidth(TEXT_FIELD_BIG.value());
		main.addFormItem(displayedName, msg.getMessage("displayedNameF"))
				.add(TooltipFactory.get(msg.getMessage("CredentialDefinition.displayedNameDescription")));

		LocalizedTextAreaDetails description = new LocalizedTextAreaDetails(
				msg.getEnabledLocales().values(), msg.getLocale());
		description.setWidth(TEXT_FIELD_BIG.value());
		main.addFormItem(description, msg.getMessage("descriptionF"));

		if (edit)
		{
			newCredState = new Select<>();
			newCredState.setItemLabelGenerator(item -> msg.getMessage("DesiredCredentialStatus." + item.name()));
			newCredState.setItems(LocalCredentialState.values());
			newCredState.setValue(LocalCredentialState.outdated);
			newCredState.setWidth(TEXT_FIELD_MEDIUM.value());
			main.addFormItem(newCredState, msg.getMessage("CredentialDefinition.replacementState"));
		}

		credentialType = new ComboBox<>();
		credentialType.setWidth(TEXT_FIELD_MEDIUM.value());

		credentialType.setItems(supportedTypes);
		main.addFormItem(credentialType, msg.getMessage("CredentialDefinition.type"));

		credentialEditorPanel = new Panel();
		credentialEditorPanel.setMargin(false);
		main.addFormItem(credentialEditorPanel, "");

		String firstType = supportedTypes.iterator().next();
		CredentialDefinition cd = initial == null ? new CredentialDefinition(
				firstType, msg.getMessage("CredentialDefinition.defaultName"), new I18nString(),
				new I18nString("")) : initial;
		if (edit)
		{
			name.setReadOnly(true);
			credentialType.setReadOnly(true);
			setCredentialEditor(initial.getConfiguration(), initial.getTypeId());
		} else
			setCredentialEditor(null, firstType);

		binder = new Binder<>(CredentialDefinition.class);
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("name");
		binder.forField(displayedName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind("displayedName");
		binder.forField(description)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind("description");
		binder.bind(credentialType, "typeId");
		binder.setBean(cd);
		credentialType.addValueChangeListener(event -> setCredentialEditor(null, credentialType.getValue()));

		getContent().add(new VerticalLayout(main, createActionLayout(msg, edit, CredentialsView.class, this::onConfirm)));
	}

	private void setCredentialEditor(String state, String type)
	{
		CredentialEditorFactory edFact = credentialEditorReg.getFactory(type);
		cdEd = edFact.creteCredentialDefinitionEditor();
		Component editor = cdEd.getEditor(state);
		credentialEditorPanel.removeAll();
		credentialEditorPanel.add(editor);
	}

	private void onConfirm()
	{
		binder.validate();
		if(binder.isValid())
		{
			String credConfig;
			try
			{
				credConfig = cdEd.getCredentialDefinition();
			} catch (IllegalCredentialException ignored)
			{
				return;
			}
			CredentialDefinition credentialDefinition = binder.getBean();
			credentialDefinition.setConfiguration(credConfig);
			credentialDefinition.getDisplayedName().setDefaultValue(credentialDefinition.getName());
			if(edit)
				credentialsController.updateCredential(credentialDefinition, newCredState.getValue(), bus);
			else
				credentialsController.addCredential(credentialDefinition, bus);
			UI.getCurrent().navigate(CredentialsView.class);
		}
	}
}
