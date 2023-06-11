/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password.web;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.vaadin.data.Binder;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.composite.password.CompositePasswordProperties;
import pl.edu.icm.unity.composite.password.CompositePasswordProperties.VerificatorTypes;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.composite.password.CompositePasswordVerificator;
import pl.edu.icm.unity.ldap.client.console.LdapAuthenticatorEditorFactory;
import pl.edu.icm.unity.pam.web.PamAuthenticatorEditorFactory;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.extensions.PasswordRetrievalProperties;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Composite password authenticator editor
 * 
 * @author P.Piernik
 *
 */
class CompositePasswordAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private MessageSource msg;
	private Collection<CredentialDefinition> credentialDefinitions;
	private PamAuthenticatorEditorFactory pamFactory;
	private LdapAuthenticatorEditorFactory ldapFactory;

	private RemoteAuthenticatorsComponent remoteAuthn;
	private Binder<CompositePasswordConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;

	CompositePasswordAuthenticatorEditor(MessageSource msg,
			Collection<CredentialDefinition> credentialDefinitions,
			PamAuthenticatorEditorFactory pamFactory, LdapAuthenticatorEditorFactory ldapFactory)
	{
		super(msg);
		this.msg = msg;
		this.credentialDefinitions = credentialDefinitions;
		this.pamFactory = pamFactory;
		this.ldapFactory = ldapFactory;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		this.subViewSwitcher = switcher;

		boolean editMode = init(msg.getMessage("CompositePasswordAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(CompositePasswordConfiguration.class);

		ChipsWithDropdown<String> localCredentials = new ChipsWithDropdown<>(i -> i, true);
		localCredentials.setItems(credentialDefinitions.stream()
				.filter(c -> c.getTypeId().equals(PasswordVerificator.NAME)).map(c -> c.getName())
				.collect(Collectors.toList()));
		localCredentials.setCaption(msg.getMessage("CompositePasswordAuthenticatorEditor.localCredentials"));
		configBinder.forField(localCredentials).bind("localCredentials");
		remoteAuthn = new RemoteAuthenticatorsComponent();
		remoteAuthn.setCaption(msg.getMessage("CompositePasswordAuthenticatorEditor.remoteAuthenticators"));
		configBinder.forField(remoteAuthn).bind("remoteAuthenticators");

		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();

		CompositePasswordConfiguration config = new CompositePasswordConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}
		configBinder.setBean(config);

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.addComponent(name);
		header.addComponent(localCredentials);
		header.addComponent(remoteAuthn);

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(header);
		main.addComponent(interactiveLoginSettings);

		return main;
	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);
		
		I18nTextField retrievalName = new I18nTextField(msg);
		retrievalName.setCaption(msg.getMessage("CompositePasswordAuthenticatorEditor.passwordName"));
		configBinder.forField(retrievalName).bind("retrievalName");
		
		interactiveLoginSettings.addComponent(retrievalName);
		CollapsibleLayout wrapper = new CollapsibleLayout(
				msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);

		return wrapper;
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
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), CompositePasswordVerificator.NAME, getConfiguration(),
				null);

	}

	public static class CompositePasswordConfiguration
	{
		private I18nString retrievalName;
		private List<String> localCredentials;
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
		private GridWithActionColumn<SimpleAuthenticatorInfo> remoteAuthnList;
		private VerticalLayout main;

		public RemoteAuthenticatorsComponent()
		{
			initUI();
		}

		public void setValue(List<SimpleAuthenticatorInfo> authenticators)
		{
			for (SimpleAuthenticatorInfo info : authenticators)
			{
				remoteAuthnList.addElement(info);
			}
		}

		private void initUI()
		{
			main = new VerticalLayout();
			main.setMargin(false);

			Button addPam = new Button(msg.getMessage("RemoteAuthenticatorsComponent.addPam"));
			addPam.addClickListener(getAddButtonClickListener(VerificatorTypes.pam));
			addPam.setIcon(Images.add.getResource());
			addPam.addStyleName(Styles.buttonAction.name());
			
			Button addLdap = new Button(msg.getMessage("RemoteAuthenticatorsComponent.addLdap"));
			addLdap.setIcon(Images.add.getResource());
			addLdap.addStyleName(Styles.buttonAction.name());
			addLdap.addClickListener(getAddButtonClickListener(VerificatorTypes.ldap));

			HorizontalLayout buttons = new HorizontalLayout();
			buttons.setMargin(false);
			buttons.addComponents(addPam, addLdap);

			main.addComponent(buttons);
			main.setComponentAlignment(buttons, Alignment.MIDDLE_RIGHT);

			remoteAuthnList = new GridWithActionColumn<>(msg, getActionsHandlers());
			remoteAuthnList.addColumn(t -> t.name, msg.getMessage("RemoteAuthenticatorsComponent.name"),
					10);
			remoteAuthnList.addColumn(t -> t.type.toString(),
					msg.getMessage("RemoteAuthenticatorsComponent.type"), 50);

			main.addComponent(remoteAuthnList);
		}

		private ClickListener getAddButtonClickListener(VerificatorTypes forType)
		{
			return e -> {
				gotoEditRemoteAuthSubView(
						forType.equals(VerificatorTypes.ldap) ? ldapFactory : pamFactory, null,
						remoteAuthnList.getElements().stream().map(p -> p.name)
								.collect(Collectors.toSet()),
						c -> {
							subViewSwitcher.exitSubViewAndShowUpdateInfo();
							remoteAuthnList.addElement(new SimpleAuthenticatorInfo(forType,
									c.id, c.configuration));
						});
			};
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
						remoteAuthnList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						remoteAuthnList.focus();
					}, subViewSwitcher);
			subViewSwitcher.goToSubView(subView);

		}

		private List<SingleActionHandler<SimpleAuthenticatorInfo>> getActionsHandlers()
		{
			SingleActionHandler<SimpleAuthenticatorInfo> edit = SingleActionHandler
					.builder4Edit(msg, SimpleAuthenticatorInfo.class).withHandler(r -> {
						SimpleAuthenticatorInfo edited = r.iterator().next();
						gotoEditRemoteAuthSubView(
								edited.type.equals(VerificatorTypes.ldap) ? ldapFactory
										: pamFactory,
								edited,
								remoteAuthnList.getElements().stream()
										.filter(p -> p.name != edited.name)
										.map(p -> p.name)
										.collect(Collectors.toSet()),
								c -> {
									remoteAuthnList.replaceElement(edited,
											new SimpleAuthenticatorInfo(
													edited.type,
													c.id,
													c.configuration));
									subViewSwitcher.exitSubView();
								});
					}

					).build();

			SingleActionHandler<SimpleAuthenticatorInfo> remove = SingleActionHandler
					.builder4Delete(msg, SimpleAuthenticatorInfo.class).withHandler(r -> {
						remoteAuthnList.removeElement(r.iterator().next());
						fireChange();
						remoteAuthnList.focus();
					}).build();

			return Arrays.asList(edit, remove);
		}

		@Override
		public List<SimpleAuthenticatorInfo> getValue()
		{
			return remoteAuthnList.getElements();
		}

		@Override
		protected Component initContent()
		{
			return main;
		}

		@Override
		protected void doSetValue(List<SimpleAuthenticatorInfo> value)
		{
			remoteAuthnList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<SimpleAuthenticatorInfo>>(this,
					remoteAuthnList.getElements(), true));
		}
	}

	public static class SimpleAuthenticatorInfo
	{
		public final VerificatorTypes type;
		public final String name;
		public final String config;

		public SimpleAuthenticatorInfo(VerificatorTypes type, String name, String config)
		{
			this.type = type;
			this.name = name;
			this.config = config;
		}
	}

}
