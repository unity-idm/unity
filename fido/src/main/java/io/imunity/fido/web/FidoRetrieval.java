/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.fido.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.fido.FidoExchange;
import io.imunity.fido.service.FidoCredentialVerificator;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.extensions.SMSRetrievalProperties;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

@PrototypeComponent
public class FidoRetrieval extends AbstractCredentialRetrieval<FidoExchange> implements VaadinAuthentication
{
	private final Logger log = Log.getLogger(Log.U_SERVER_WEB, FidoRetrieval.class);
	public static final String NAME = "vaadin-fido";
	public static final String DESC = "fido.desc";

	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private I18nString name;
	private final CredentialEditorRegistry credEditorReg;
	private String configuration;

	@Autowired
	public FidoRetrieval(MessageSource msg, CredentialEditorRegistry credEditorReg, NotificationPresenter notificationPresenter)
	{	
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
		this.notificationPresenter = notificationPresenter;
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		return configuration;
	}

	@Override
	public void setSerializedConfiguration(String configuration)
	{
		this.configuration = configuration;
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(configuration));
			SMSRetrievalProperties config = new SMSRetrievalProperties(properties);
			name = config.getLocalizedString(msg, SMSRetrievalProperties.NAME);
			if (name.isEmpty())
				name = new I18nString("fido.title", msg);
	
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the fido retrieval can not be parsed", e);
		}
	}
	
	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context, AuthenticatorStepContext authenticatorContext)
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new FidoRetrievalUI(credEditorReg.getEditor(FidoCredentialVerificator.NAME)));
	}

	@Override
	public boolean supportsGrid()
	{
		return false;
	}

	@Override
	public boolean isMultiOption()
	{
		return false;
	}

	private AuthenticationRetrievalContext getContext()
	{
		return AuthenticationRetrievalContext.builder().build();
	}
	
	private class FidoRetrievalComponent extends VerticalLayout implements Focusable
	{
		private AuthenticationCallback callback;
		private TextField usernameField;
		private int tabIndex;
		private VerticalLayout visiblePart;
		private FidoComponent fidoComponent;
		private Long entityId;
		private Button authenticateButton;
		
		public FidoRetrievalComponent(CredentialEditor credEditor)
		{
			initUI();
		}

		private void initUI()
		{
			addClassName("u-fidoRetrieval");
			addClassName("u-fidoRetrievalLayout");
			setMargin(false);
			setPadding(false);
			getStyle().set("gap", "0");

			fidoComponent = FidoComponent.builder(msg)
					.fidoExchange(credentialExchange)
					.showSuccessNotification(false)
					.authenticationResultListener(this::setAuthenticationResult)
					.notificationPresenter(notificationPresenter)
					.build();
			add(fidoComponent.getComponent());

			visiblePart = new VerticalLayout();
			visiblePart.setMargin(false);
			visiblePart.setPadding(false);
			visiblePart.getStyle().set("gap", "0");

			usernameField = new TextField();
			usernameField.setWidthFull();
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			usernameField.addClassName("u-authnTextField");
			usernameField.addClassName("u-passwordUsernameField");
			visiblePart.add(usernameField);

			authenticateButton = new Button(msg.getMessage("Fido.WebRetrieval.signIn"));
			authenticateButton.addClickListener(event -> triggerAuthentication());
			authenticateButton.addClassName("u-passwordSignInButton");
			authenticateButton.setWidthFull();
			authenticateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			visiblePart.add(authenticateButton);

			usernameField.addFocusListener(event ->
			{
				ShortcutRegistration shortcutRegistration = authenticateButton.addClickShortcut(Key.ENTER);
				usernameField.addBlurListener(e -> shortcutRegistration.remove());

			});

			add(visiblePart);
		}

		private void triggerAuthentication()
		{
			String username = usernameField.getValue();
			fidoComponent.invokeAuthentication(entityId, username);
		}

		private void setAuthenticationResult(AuthenticationResult authenticationResult)
		{
			log.debug("Enter setAuthenticationResult: {}", authenticationResult);
			if (authenticationResult.getStatus() == Status.success)
			{
				clear();
				setEnabled(false);
				callback.onCompletedAuthentication(authenticationResult, getContext());
			} else if (authenticationResult.getStatus() == Status.notApplicable)
			{
				clear();
				usernameField.focus();
				AuthenticationResult exposedError = LocalAuthenticationResult.failed(
						new ResolvableError("Fido.invalidUsername"));
				callback.onCompletedAuthentication(exposedError, getContext());
			} else
			{
				usernameField.focus();
				AuthenticationResult exposedError = LocalAuthenticationResult.failed(
						new ResolvableError("Fido.authFailed"));
				callback.onCompletedAuthentication(exposedError, getContext());
			}
		}

		@Override
		public void focus()
		{
			if (entityId == null)
				usernameField.focus();
			else
				authenticateButton.focus();
		}

		@Override
		public int getTabIndex()
		{
			return tabIndex;
		}

		@Override
		public void setTabIndex(int tabIndex)
		{
			this.tabIndex = tabIndex;
		}

		public void setCallback(AuthenticationCallback callback)
		{
			this.callback = callback;
		}

		void setAuthenticatedEntity(long entityId)
		{
			this.entityId = entityId;
			visiblePart.remove(usernameField);
		}

		private void clear()
		{
			usernameField.setValue("");
		}
	}

	private class FidoRetrievalUI implements VaadinAuthenticationUI
	{
		private FidoRetrievalComponent theComponent;

		public FidoRetrievalUI(CredentialEditor credEditor)
		{
			this.theComponent = new FidoRetrievalComponent(credEditor);
		}

		@Override
		public void setAuthenticationCallback(AuthenticationCallback callback)
		{
			theComponent.setCallback(callback);
		}
		
		@Override
		public Component getComponent()
		{
			return theComponent;
		}

		@Override
		public String getLabel()
		{
			return name.getValue(msg);
		}

		@Override
		public Image getImage()
		{
			return null;
		}

		@Override
		public void clear()
		{
			theComponent.clear();
		}

		/**
		 * Simple: there is only one authN option in this authenticator
		 * so we can return any constant id.
		 */
		@Override
		public String getId()
		{
			return "fido";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
			theComponent.setAuthenticatedEntity(authenticatedEntity.getId());
		}

		@Override
		public Set<String> getTags()
		{
			return Collections.emptySet();
		}

		@Override
		public void disableCredentialReset()
		{
		}
	}

	@org.springframework.stereotype.Component
	public static class Factory extends AbstractCredentialRetrievalFactory<FidoRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<FidoRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, FidoExchange.ID);
		}
	}
}
