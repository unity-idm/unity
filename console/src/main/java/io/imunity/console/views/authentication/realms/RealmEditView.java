/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.realms;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.message.MessageSource;

import javax.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "edit")
@Route(value = "/realms/edit", layout = ConsoleMenu.class)
public class RealmEditView extends ConsoleViewComponent
{
	private final RealmsController realmsController;
	private final MessageSource msg;

	private Binder<AuthenticationRealm> binder;
	private boolean edit;

	public RealmEditView(MessageSource msg, RealmsController realmsController)
	{
		this.realmsController = realmsController;
		this.msg = msg;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String realmName)
	{
		getContent().removeAll();

		AuthenticationRealmEntry certificateEntry;
		if(realmName == null)
		{
			certificateEntry = new AuthenticationRealmEntry();
			edit = false;
		}
		else
		{
			certificateEntry = realmsController.getRealm(realmName);
			edit = true;
		}
		initUI(certificateEntry);
	}

	void initUI(AuthenticationRealmEntry toEdit)
	{
		TextField name = new TextField();
		name.setWidth("var(--vaadin-text-field-big)");
		TextField description = new TextField();
		description.setWidth("var(--vaadin-text-field-big)");

		IntegerField blockAfterUnsuccessfulLogins = getIntegerField();
		IntegerField blockFor = getIntegerField();

		ComboBox<RememberMePolicy> rememberMePolicy = new ComboBox<>();
		rememberMePolicy.setItems(RememberMePolicy.values());
		rememberMePolicy.setWidth("var(--vaadin-text-field-big)");

		IntegerField allowForRememberMeDays = getIntegerField();
		IntegerField maxInactivity = getIntegerField();
		maxInactivity.setMax(99999);

		configBinder(toEdit, name, description, blockAfterUnsuccessfulLogins, blockFor, rememberMePolicy, allowForRememberMeDays, maxInactivity);
		FormLayout mainLayout = createMainLayout(toEdit, name, description, blockAfterUnsuccessfulLogins, blockFor, rememberMePolicy, allowForRememberMeDays, maxInactivity);
		getContent().add(new VerticalLayout(mainLayout, createActionLayout()));
	}

	private IntegerField getIntegerField()
	{
		IntegerField integerField = new IntegerField();
		integerField.setMin(1);
		integerField.setMax(999);
		integerField.setHasControls(true);
		return integerField;
	}

	private FormLayout createMainLayout(AuthenticationRealmEntry toEdit, TextField name, TextField description, IntegerField blockAfterUnsuccessfulLogins, IntegerField blockFor, ComboBox<RememberMePolicy> rememberMePolicy, IntegerField allowForRememberMeDays, IntegerField maxInactivity)
	{
		FormLayout mainLayout = new FormLayout();
		mainLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainLayout.addClassName("big-vaadin-form-item");
		mainLayout.addFormItem(name, msg.getMessage("AuthenticationRealm.name"))
				.add(getTooltipIcon("AuthenticationRealm.name.tooltip"));
		mainLayout.addFormItem(description, msg.getMessage("AuthenticationRealm.description"))
				.add(getTooltipIcon("AuthenticationRealm.description.tooltip"));
		mainLayout.addFormItem(blockAfterUnsuccessfulLogins, msg.getMessage("AuthenticationRealm.blockAfterUnsuccessfulLogins"))
				.add(getTooltipIcon("AuthenticationRealm.blockAfterUnsuccessfulLogins.tooltip"));
		mainLayout.addFormItem(blockFor, msg.getMessage("AuthenticationRealm.blockFor"))
				.add(getTooltipIcon("AuthenticationRealm.blockFor.tooltip"));
		mainLayout.addFormItem(rememberMePolicy, msg.getMessage("AuthenticationRealm.rememberMePolicy"))
				.add(getTooltipIcon("AuthenticationRealm.rememberMePolicy.tooltip"));
		mainLayout.addFormItem(allowForRememberMeDays, msg.getMessage("AuthenticationRealm.allowForRememberMeDays"))
				.add(getTooltipIcon("AuthenticationRealm.allowForRememberMeDays.tooltip"));
		mainLayout.addFormItem(maxInactivity, msg.getMessage("AuthenticationRealm.maxInactivity"))
				.add(getTooltipIcon("AuthenticationRealm.maxInactivity.tooltip"));
		if (!toEdit.endpoints.isEmpty())
		{
			VerticalLayout field = new VerticalLayout(toEdit.endpoints.stream().map(Label::new).toArray(Component[]::new));
			mainLayout.addFormItem(field, msg.getMessage("AuthenticationRealm.endpoints"));
		}

		return mainLayout;
	}

	private void configBinder(AuthenticationRealmEntry toEdit, TextField name, TextField description, IntegerField blockAfterUnsuccessfulLogins, IntegerField blockFor, ComboBox<RememberMePolicy> rememberMePolicy, IntegerField allowForRememberMeDays, IntegerField maxInactivity)
	{
		binder = new Binder<>(AuthenticationRealm.class);

		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");

		binder.forField(blockAfterUnsuccessfulLogins)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("blockAfterUnsuccessfulLogins");
		binder.forField(blockFor).asRequired(msg.getMessage("fieldRequired"))
				.bind("blockFor");

		binder.forField(rememberMePolicy).asRequired(msg.getMessage("fieldRequired"))
				.bind("rememberMePolicy");
		binder.forField(allowForRememberMeDays).asRequired(msg.getMessage("fieldRequired"))
				.bind("allowForRememberMeDays");

		binder.forField(maxInactivity).asRequired(msg.getMessage("fieldRequired"))
				.bind("maxInactivity");
		binder.setBean(toEdit.realm);
	}

	private void onConfirm()
	{
		binder.validate();
		if(binder.isValid())
		{
			AuthenticationRealm bean = binder.getBean();
			if(edit)
				realmsController.updateRealm(bean);
			else
				realmsController.addRealm(bean);
			UI.getCurrent().navigate(RealmsView.class);
		}
	}

	private HorizontalLayout createActionLayout()
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(RealmsView.class));
		Button updateButton = new Button(edit ? msg.getMessage("update") : msg.getMessage("create"));
		updateButton.addClickListener(event -> onConfirm());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		return new HorizontalLayout(cancelButton, updateButton);
	}

	private Component getTooltipIcon(String code)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		icon.setTooltipText(msg.getMessage(code));
		icon.getStyle().set("align-self", "center");
		icon.getStyle().set("margin-left", "var(--small-margin)");
		return icon;
	}

}