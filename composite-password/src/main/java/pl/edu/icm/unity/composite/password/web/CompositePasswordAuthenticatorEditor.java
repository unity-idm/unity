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
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.composite.password.CompositePasswordProperties;
import pl.edu.icm.unity.composite.password.CompositePasswordProperties.VerificatorTypes;
import pl.edu.icm.unity.composite.password.CompositePasswordVerificator;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.pam.web.PamAuthenticatorEditorFactory;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.extensions.PasswordRetrievalProperties;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Composite password authenticator editor
 * @author P.Piernik
 *
 */
public class CompositePasswordAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{

	private UnityMessageSource msg;
	private Collection<CredentialDefinition> credentialDefinitions;
	private Binder<CompositePasswordConfiguration> binder;
	private SubViewSwitcher subViewSwitcher;
	private PamAuthenticatorEditorFactory pamFactory;

	private RemoteAuthenticatorsComponent remoteAuthn;
	
	CompositePasswordAuthenticatorEditor(UnityMessageSource msg,
			Collection<CredentialDefinition> credentialDefinitions,
			PamAuthenticatorEditorFactory pamFactory)
	{
		super(msg);
		this.msg = msg;
		this.credentialDefinitions = credentialDefinitions;
		this.pamFactory = pamFactory;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		this.subViewSwitcher = switcher;

		boolean editMode = toEdit != null;
		setName(editMode ? toEdit.id : msg.getMessage("CompositePasswordAuthenticatorEditor.defaultName"));
		setNameReadOnly(editMode && !forceNameEditable);

		binder = new Binder<>(CompositePasswordConfiguration.class);

		ChipsWithDropdown<String> localCredentials = new ChipsWithDropdown<>(i -> i, true);
		localCredentials.setItems(credentialDefinitions.stream()
				.filter(c -> c.getTypeId().equals(PasswordVerificator.NAME)).map(c -> c.getName())
				.collect(Collectors.toList()));
		localCredentials.setCaption(msg.getMessage("CompositePasswordAuthenticatorEditor.localCredentials"));
		binder.forField(localCredentials).bind("localCredentials");

		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();

		CompositePasswordConfiguration config = new CompositePasswordConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}
		binder.setBean(config);

		remoteAuthn = new RemoteAuthenticatorsComponent();
		remoteAuthn.setCaption(msg.getMessage("CompositePasswordAuthenticatorEditor.remoteAuthenticators"));
		remoteAuthn.setValue(binder.getBean().getRemoteAuthenticators());

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
		I18nTextField retrivalName = new I18nTextField(msg);
		retrivalName.setCaption(msg.getMessage("CompositePasswordAuthenticatorEditor.passwordName"));
		interactiveLoginSettings.addComponent(retrivalName);
		CollapsibleLayout wrapper = new CollapsibleLayout(
				msg.getMessage("CompositePasswordAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
		binder.forField(retrivalName).bind("retrivalName");
		return wrapper;
	}

	private String getConfiguration() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		binder.getBean().setRemoteAuthenticators(remoteAuthn.getRemoteAuthenticators());
		
		return binder.getBean().toProperties();
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), CompositePasswordVerificator.NAME, getConfiguration(),
				null);

	}

	public static class CompositePasswordConfiguration
	{
		private I18nString retrivalName;
		private List<String> localCredentials;
		private List<SimpleAuthenticatorInfo> remoteAuthenticators;

		public CompositePasswordConfiguration()
		{
			remoteAuthenticators = new ArrayList<>();
		}

		public I18nString getRetrivalName()
		{
			return retrivalName;
		}

		public void setRetrivalName(I18nString retrivalName)
		{
			this.retrivalName = retrivalName;
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

		public String toProperties()
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

			CompositePasswordProperties prop = new CompositePasswordProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, UnityMessageSource msg)
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
			retrivalName = passwordRetrievalProperties.getLocalizedString(msg,
					PasswordRetrievalProperties.NAME);
		}

		private String getConfigFromFile(File configFile)
		{
			try
			{
				return configFile == null ? null
						: FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
			} catch (IOException e)
			{
				return null;
			}
		}

	}

	private class RemoteAuthenticatorsComponent extends CustomComponent
	{
		private GridWithActionColumn<SimpleAuthenticatorInfo> remoteAuthnList;

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
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);

			Button addPam = new Button(msg.getMessage("RemoteAuthenticatorsComponent.addPam"));
			addPam.addClickListener(e -> {
				gotoEditPamSubView(null, remoteAuthnList.getElements().stream().map(p -> p.name)
						.collect(Collectors.toSet()), c -> {
							subViewSwitcher.exitSubView();
							remoteAuthnList.addElement(new SimpleAuthenticatorInfo(
									VerificatorTypes.pam, c.id, c.configuration));
						});
			});
			addPam.setIcon(Images.add.getResource());

			Button addLdap = new Button(msg.getMessage("RemoteAuthenticatorsComponent.addLdap"));
			addLdap.setIcon(Images.add.getResource());

			HorizontalLayout buttons = new HorizontalLayout();
			buttons.setMargin(false);
			buttons.addComponents(addPam, addLdap);

			main.addComponent(buttons);
			main.setComponentAlignment(buttons, Alignment.MIDDLE_RIGHT);

			remoteAuthnList = new GridWithActionColumn<>(msg, getActionsHandlers());
			remoteAuthnList.addColumn(t -> t.name, msg.getMessage("RemoteAuthenticatorsComponent.name"), 10);
			remoteAuthnList.addColumn(t -> t.type.toString(),
					msg.getMessage("RemoteAuthenticatorsComponent.type"), 50);

			main.addComponent(remoteAuthnList);

			setCompositionRoot(main);
		}

		private void gotoEditPamSubView(SimpleAuthenticatorInfo authenticator, Set<String> names,
				Consumer<AuthenticatorDefinition> onConfirm)
		{

			EditRemoteAuthenticatorSubView subView = new EditRemoteAuthenticatorSubView(msg, pamFactory,
					authenticator != null
							? new AuthenticatorDefinition(authenticator.name, null,
									authenticator.config, null)
							: null,
					r -> {
						onConfirm.accept(r);
						name.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						name.focus();
					}, subViewSwitcher);
			subViewSwitcher.goToSubView(subView);

		}

		private List<SingleActionHandler<SimpleAuthenticatorInfo>> getActionsHandlers()
		{
			SingleActionHandler<SimpleAuthenticatorInfo> edit = SingleActionHandler
					.builder4Edit(msg, SimpleAuthenticatorInfo.class).withHandler(r -> {
						SimpleAuthenticatorInfo edited = r.iterator().next();
						gotoEditPamSubView(edited, remoteAuthnList.getElements().stream()
								.filter(p -> p.name != edited.name).map(p -> p.name)
								.collect(Collectors.toSet()), c -> {
									remoteAuthnList.replaceEntry(edited,
											new SimpleAuthenticatorInfo(
													VerificatorTypes.pam,
													c.id,
													c.configuration));
									subViewSwitcher.exitSubView();
								});
					}

					).build();

			SingleActionHandler<SimpleAuthenticatorInfo> remove = SingleActionHandler
					.builder4Delete(msg, SimpleAuthenticatorInfo.class)
					.withHandler(r -> remoteAuthnList.removeElement(r.iterator().next())).build();

			return Arrays.asList(edit, remove);
		}
		
		public List<SimpleAuthenticatorInfo> getRemoteAuthenticators()
		{
			return remoteAuthnList.getElements();
		}
	}

	public static class SimpleAuthenticatorInfo
	{
		public final VerificatorTypes type;
		public final String name;
		public final String config;

		public SimpleAuthenticatorInfo(VerificatorTypes type, String name, String config)
		{
			super();
			this.type = type;
			this.name = name;
			this.config = config;
		}
	}

}
