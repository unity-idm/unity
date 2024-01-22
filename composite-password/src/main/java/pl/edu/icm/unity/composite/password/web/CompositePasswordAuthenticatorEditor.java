/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.auth.extensions.PasswordRetrievalProperties;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import org.apache.commons.io.FileUtils;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.composite.password.CompositePasswordProperties;
import pl.edu.icm.unity.composite.password.CompositePasswordProperties.VerificatorTypes;
import pl.edu.icm.unity.composite.password.CompositePasswordVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.ldap.client.console.LdapAuthenticatorEditorFactory;
import pl.edu.icm.unity.pam.web.PamAuthenticatorEditorFactory;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.PLUS_CIRCLE_O;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class CompositePasswordAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final MessageSource msg;
	private final Collection<CredentialDefinition> credentialDefinitions;
	private final PamAuthenticatorEditorFactory pamFactory;
	private final LdapAuthenticatorEditorFactory ldapFactory;
	private final NotificationPresenter notificationPresenter;

	private Binder<CompositePasswordConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;

	CompositePasswordAuthenticatorEditor(MessageSource msg,
			Collection<CredentialDefinition> credentialDefinitions,
			PamAuthenticatorEditorFactory pamFactory, LdapAuthenticatorEditorFactory ldapFactory, NotificationPresenter notificationPresenter)
	{
		super(msg);
		this.msg = msg;
		this.credentialDefinitions = credentialDefinitions;
		this.pamFactory = pamFactory;
		this.ldapFactory = ldapFactory;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		this.subViewSwitcher = switcher;

		boolean editMode = init(msg.getMessage("CompositePasswordAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(CompositePasswordConfiguration.class);

		MultiSelectComboBox<String> localCredentials = new MultiSelectComboBox<>();
		localCredentials.setItems(credentialDefinitions.stream()
				.filter(c -> c.getTypeId().equals(PasswordVerificator.NAME))
				.map(CredentialDefinition::getName)
				.collect(Collectors.toList()));
		localCredentials.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(localCredentials)
				.withConverter(List::copyOf, HashSet::new)
				.bind(CompositePasswordConfiguration::getLocalCredentials, CompositePasswordConfiguration::setLocalCredentials);
		RemoteAuthenticatorsComponent remoteAuthn = new RemoteAuthenticatorsComponent();
		configBinder.forField(remoteAuthn)
				.bind(CompositePasswordConfiguration::getRemoteAuthenticators, CompositePasswordConfiguration::setRemoteAuthenticators);

		AccordionPanel interactiveLoginSettings = buildInteractiveLoginSettingsSection();
		interactiveLoginSettings.setWidthFull();

		CompositePasswordConfiguration config = new CompositePasswordConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}
		configBinder.setBean(config);

		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		header.addFormItem(localCredentials, msg.getMessage("CompositePasswordAuthenticatorEditor.localCredentials"));
		header.addFormItem(remoteAuthn, msg.getMessage("CompositePasswordAuthenticatorEditor.remoteAuthenticators"));

		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.add(header, interactiveLoginSettings);

		return main;
	}

	private AccordionPanel buildInteractiveLoginSettingsSection()
	{
		FormLayout formLayout = new FormLayout();
		formLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		LocalizedTextFieldDetails retrievalName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		retrievalName.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(retrievalName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(CompositePasswordConfiguration::getRetrievalName, CompositePasswordConfiguration::setRetrievalName);
		formLayout.addFormItem(retrievalName, msg.getMessage("CompositePasswordAuthenticatorEditor.passwordName"));

		return new AccordionPanel(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"), formLayout);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();
		try
		{
			return configBinder.getBean().toProperties(msg);
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the composite-password verificator",
					e);
		}
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), CompositePasswordVerificator.NAME, getConfiguration(),
				null);

	}

	public static class CompositePasswordConfiguration
	{
		private I18nString retrievalName = new I18nString();
		private List<String> localCredentials = new ArrayList<>();
		private List<SimpleAuthenticatorInfo> remoteAuthenticators;

		public CompositePasswordConfiguration()
		{
			remoteAuthenticators = new ArrayList<>();
		}

		public String toProperties(MessageSource msg)
		{

			Properties raw = new Properties();

			String prefix = CompositePasswordProperties.PREFIX;

			int i = 1;
			if (localCredentials != null)
			{
				for (String credential : localCredentials)
				{
					raw.put(prefix + CompositePasswordProperties.VERIFICATORS + i + "."
							+ CompositePasswordProperties.VERIFICATOR_TYPE,
							CompositePasswordProperties.VerificatorTypes.password
									.toString());
					raw.put(prefix + CompositePasswordProperties.VERIFICATORS + i + "."
							+ CompositePasswordProperties.VERIFICATOR_CREDENTIAL,
							credential);
					i++;
				}
			}

			for (SimpleAuthenticatorInfo remoteAuth : remoteAuthenticators)
			{
				raw.put(prefix + CompositePasswordProperties.VERIFICATORS + i + "."
						+ CompositePasswordProperties.VERIFICATOR_NAME, remoteAuth.name);

				raw.put(prefix + CompositePasswordProperties.VERIFICATORS + i + "."
						+ CompositePasswordProperties.VERIFICATOR_TYPE,
						remoteAuth.type.toString());

				raw.put(prefix + CompositePasswordProperties.VERIFICATORS + i + "."
						+ CompositePasswordProperties.VERIFICATOR_CONFIG_EMBEDDED,
						remoteAuth.config);
				i++;
			}

			if (getRetrievalName() != null)
			{
				getRetrievalName().toProperties(raw,
						PasswordRetrievalProperties.P + PasswordRetrievalProperties.NAME, msg);
			}

			CompositePasswordProperties prop = new CompositePasswordProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, MessageSource msg)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties));
			} catch (IOException e)
			{
				throw new InternalException(
						"Invalid configuration of the composite-password verificator", e);
			}

			CompositePasswordProperties compositePasswordProp = new CompositePasswordProperties(raw);
			localCredentials = new ArrayList<>();
			Set<String> verfKeys = compositePasswordProp
					.getStructuredListKeys(CompositePasswordProperties.VERIFICATORS);
			for (String verfKey : verfKeys)
			{
				VerificatorTypes type = compositePasswordProp.getEnumValue(
						verfKey + CompositePasswordProperties.VERIFICATOR_TYPE,
						VerificatorTypes.class);
				if (type.equals(VerificatorTypes.password))
				{
					localCredentials.add(compositePasswordProp.getValue(
							verfKey + CompositePasswordProperties.VERIFICATOR_CREDENTIAL));
				} else
				{
					String name = compositePasswordProp.getValue(
							verfKey + CompositePasswordProperties.VERIFICATOR_NAME);
					if (name == null)
					{

						name = verfKey.split("\\.")[1];
					}

					String stringConfig = null;
					if (compositePasswordProp.isSet(
							verfKey + CompositePasswordProperties.VERIFICATOR_CONFIG))
					{
						File config = compositePasswordProp.getFileValue(verfKey
								+ CompositePasswordProperties.VERIFICATOR_CONFIG,
								false);
						stringConfig = getConfigFromFile(config);
					} else
					{
						stringConfig = compositePasswordProp.getValue(verfKey
								+ CompositePasswordProperties.VERIFICATOR_CONFIG_EMBEDDED);
					}

					remoteAuthenticators.add(new SimpleAuthenticatorInfo(type, name, stringConfig));

				}

			}

			PasswordRetrievalProperties passwordRetrievalProperties = new PasswordRetrievalProperties(raw);
			setRetrievalName(passwordRetrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg,
					PasswordRetrievalProperties.NAME));
		}

		private String getConfigFromFile(File configFile)
		{
			try
			{
				return configFile == null ? null
						: FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
			} catch (IOException e)
			{
				throw new InternalException("Can not read remote authenticator config file", e);
			}
		}

		public I18nString getRetrievalName()
		{
			return retrievalName;
		}

		public void setRetrievalName(I18nString retrievalName)
		{
			this.retrievalName = retrievalName;
		}

		public List<String> getLocalCredentials()
		{
			return localCredentials;
		}

		public void setLocalCredentials(List<String> localCredentials)
		{
			this.localCredentials = localCredentials;
		}

		public List<SimpleAuthenticatorInfo> getRemoteAuthenticators()
		{
			return remoteAuthenticators;
		}

		public void setRemoteAuthenticators(List<SimpleAuthenticatorInfo> remoteAuthenticators)
		{
			this.remoteAuthenticators = remoteAuthenticators;
		}
	}

	private class RemoteAuthenticatorsComponent extends CustomField<List<SimpleAuthenticatorInfo>>
	{
		private GridWithActionColumn<SimpleAuthenticatorInfo> remoteAuthnGrid;
		private List<SimpleAuthenticatorInfo> remoteAuthnList;

		public RemoteAuthenticatorsComponent()
		{
			initUI();
		}

		@Override
		protected List<SimpleAuthenticatorInfo> generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(List<SimpleAuthenticatorInfo> simpleAuthenticatorInfos)
		{
			setValue(simpleAuthenticatorInfos);
		}

		@Override
		public void setValue(List<SimpleAuthenticatorInfo> authenticators)
		{
			remoteAuthnList.addAll(authenticators);
		}

		private void initUI()
		{
			VerticalLayout main = new VerticalLayout();
			main.setWidth("35em");
			main.setHeight("20em");
			main.setMargin(false);

			Button addPam = new Button(msg.getMessage("RemoteAuthenticatorsComponent.addPam"));
			addPam.addClickListener(e -> getAddButtonClickListener(VerificatorTypes.pam));
			addPam.setIcon(PLUS_CIRCLE_O.create());

			Button addLdap = new Button(msg.getMessage("RemoteAuthenticatorsComponent.addLdap"));
			addLdap.setIcon(PLUS_CIRCLE_O.create());
			addLdap.addClickListener(e -> getAddButtonClickListener(VerificatorTypes.ldap));

			HorizontalLayout buttons = new HorizontalLayout();
			buttons.setMargin(false);
			buttons.add(addPam, addLdap);

			main.add(buttons);
			main.setAlignItems(FlexComponent.Alignment.END);

			remoteAuthnList = new ArrayList<>();
			remoteAuthnGrid = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			remoteAuthnGrid.enableRowReordering(this::updateValue);
			remoteAuthnGrid.addColumn(t -> t.name)
					.setHeader(msg.getMessage("RemoteAuthenticatorsComponent.name"));
			remoteAuthnGrid.addColumn(t -> t.type.toString())
					.setHeader(msg.getMessage("RemoteAuthenticatorsComponent.type"));
			remoteAuthnGrid.setItems(remoteAuthnList);

			main.add(remoteAuthnGrid);
			add(main);
		}

		private void getAddButtonClickListener(VerificatorTypes forType)
		{
				gotoEditRemoteAuthSubView(
						forType.equals(VerificatorTypes.ldap) ? ldapFactory : pamFactory, null,
						remoteAuthnList.stream().map(p -> p.name)
								.collect(Collectors.toSet()),
						c -> {
							subViewSwitcher.exitSubViewAndShowUpdateInfo();
							remoteAuthnList.add(new SimpleAuthenticatorInfo(forType,
									c.id, c.configuration));
							remoteAuthnGrid.getDataProvider().refreshAll();
						});
		}

		private void gotoEditRemoteAuthSubView(AuthenticatorEditorFactory factory,
				SimpleAuthenticatorInfo authenticator, Set<String> names,
				Consumer<AuthenticatorDefinition> onConfirm)
		{

			EditRemoteAuthenticatorSubView subView = new EditRemoteAuthenticatorSubView(msg, factory,
					authenticator != null
							? new AuthenticatorDefinition(authenticator.name, null,
									authenticator.config, null)
							: null,
					r -> {
						onConfirm.accept(r);
						fireChange();
						remoteAuthnGrid.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						remoteAuthnGrid.focus();
					}, subViewSwitcher, notificationPresenter);
			subViewSwitcher.goToSubView(subView);

		}

		private List<SingleActionHandler<SimpleAuthenticatorInfo>> getActionsHandlers()
		{
			SingleActionHandler<SimpleAuthenticatorInfo> edit = SingleActionHandler
					.builder4Edit(msg::getMessage, SimpleAuthenticatorInfo.class).withHandler(r -> {
						SimpleAuthenticatorInfo edited = r.iterator().next();
						gotoEditRemoteAuthSubView(
								edited.type.equals(VerificatorTypes.ldap) ? ldapFactory
										: pamFactory,
								edited,
								remoteAuthnList.stream()
										.filter(p -> !Objects.equals(p.name, edited.name))
										.map(p -> p.name)
										.collect(Collectors.toSet()),
								c -> {
									remoteAuthnList.remove(edited);
									remoteAuthnList.add(
											new SimpleAuthenticatorInfo(
													edited.type,
													c.id,
													c.configuration));
									subViewSwitcher.exitSubView();
									remoteAuthnGrid.getDataProvider().refreshAll();
								});
					}

					).build();

			SingleActionHandler<SimpleAuthenticatorInfo> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, SimpleAuthenticatorInfo.class).withHandler(r -> {
						remoteAuthnList.remove(r.iterator().next());
						fireChange();
						remoteAuthnGrid.focus();
						remoteAuthnGrid.getDataProvider().refreshAll();
					}).build();

			SingleActionHandler<SimpleAuthenticatorInfo> reorder = new SingleActionHandler.Builder<SimpleAuthenticatorInfo>()
					.withIcon(VaadinIcon.RESIZE_H)
					.build();
			return Arrays.asList(reorder, edit, remove);
		}

		@Override
		public List<SimpleAuthenticatorInfo> getValue()
		{
			return List.copyOf(remoteAuthnList);
		}

		private void fireChange()
		{
			fireEvent(new ComponentValueChangeEvent<>(this, this,
					List.copyOf(remoteAuthnList), false));
		}
	}

	public record SimpleAuthenticatorInfo(
			VerificatorTypes type,
			String name,
			String config)
	{}

}
