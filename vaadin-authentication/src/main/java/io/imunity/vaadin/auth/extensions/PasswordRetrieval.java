/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.AuthNGridTextWrapper;
import io.imunity.vaadin.auth.CredentialResetLauncher;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.extensions.credreset.password.PasswordCredentialResetController;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;

import java.io.StringReader;
import java.util.*;

/**
 * Retrieves passwords using a Vaadin widget.
 */
@PrototypeComponent
public class PasswordRetrieval extends AbstractCredentialRetrieval<PasswordExchange> implements VaadinAuthentication
{
	public static final String NAME = "vaadin-password";
	public static final String DESC = "WebPasswordRetrievalFactory.desc";
	
	private final Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordRetrieval.class);
	private final MessageSource msg;
	private I18nString name;
	private String registrationFormForUnknown;
	private boolean enableAssociation;
	private final CredentialEditorRegistry credEditorReg;
	private String configuration;
	private NotificationPresenter notificationPresenter;

	@Autowired
	public PasswordRetrieval(MessageSource msg, CredentialEditorRegistry credEditorReg, NotificationPresenter notificationPresenter)
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
			PasswordRetrievalProperties config = new PasswordRetrievalProperties(properties);
			name = config.getLocalizedString(msg, PasswordRetrievalProperties.NAME);
			if (name.isEmpty())
				name = new I18nString("WebPasswordRetrieval.password", msg);
			registrationFormForUnknown = config.getValue(
					PasswordRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN);
			enableAssociation = config.getBooleanValue(PasswordRetrievalProperties.ENABLE_ASSOCIATION);
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based password retrieval can not be parsed or is invalid", e);
		}
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context, AuthenticatorStepContext authenticatorContext)
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new PasswordRetrievalUI(credEditorReg.getEditor(PasswordVerificator.NAME)));
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
		return AuthenticationRetrievalContext.builder().withSupportOnlySecondFactorReseting(true).build();
	}
	
	private class PasswordRetrievalComponent extends VerticalLayout implements Focusable
	{
		private final CredentialEditor credEditor;
		private AuthenticationCallback callback;
		private String presetAuthenticatedIdentity;
		
		private TextField usernameField;
		private PasswordField passwordField;
		private int tabIndex;
		private LinkButton reset;
		private CredentialResetLauncher credResetLauncher;
		private Button authenticateButton;
		private final NotificationPresenter notificationPresenter;

		public PasswordRetrievalComponent(CredentialEditor credEditor, NotificationPresenter notificationPresenter)
		{
			this.credEditor = credEditor;
			this.notificationPresenter = notificationPresenter;
			initUI();
		}

		private void initUI()
		{
			setMargin(false);
			setPadding(false);
			getStyle().set("gap", "0");

			usernameField = new TextField();
			usernameField.setWidthFull();
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			usernameField.addClassName("u-authnTextField");
			usernameField.addClassName("u-passwordUsernameField");
			add(usernameField);
			
			String label = name.getValue(msg);
			passwordField = new PasswordField();
			passwordField.setWidthFull();
			passwordField.setPlaceholder(label);
			passwordField.addClassName("u-authnTextField");
			passwordField.addClassName("u-passwordField");
			add(passwordField);
			
			
			authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
			authenticateButton.addClassName("u-passwordSignInButton");
			authenticateButton.addClickListener(event -> triggerAuthentication());
			authenticateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			authenticateButton.setWidthFull();
			add(authenticateButton);

			passwordField.addFocusListener(event ->
			{
				ShortcutRegistration shortcutRegistration = authenticateButton.addClickShortcut(Key.ENTER);
				passwordField.addBlurListener(e -> shortcutRegistration.remove());
			});

			PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings(
					JsonUtil.parse(credentialExchange
							.getCredentialResetBackend()
							.getSettings()));
			if (settings.isEnabled())
			{
				reset = new LinkButton(msg.getMessage("WebPasswordRetrieval.forgottenPassword"), event -> showResetDialog());
				AuthNGridTextWrapper resetWrapper = new AuthNGridTextWrapper(reset, Alignment.END);
				resetWrapper.addClassName("u-authn-forgotPassword");
				add(resetWrapper);
			}
		}

		private void triggerAuthentication()
		{
			String username = presetAuthenticatedIdentity == null ? usernameField.getValue() : 
				presetAuthenticatedIdentity;
			String password = passwordField.getValue();

			if (password.equals(""))
			{
				notificationPresenter.showErrorAutoClosing(msg.getMessage("AuthenticationUI.authnErrorTitle"),
						msg.getMessage("WebPasswordRetrieval.noPassword"));
			} else if (username.equals(""))
			{
				notificationPresenter.showErrorAutoClosing(msg.getMessage("AuthenticationUI.authnErrorTitle"),
						msg.getMessage("WebPasswordRetrieval.noUser"));
			} else 
			{
				callback.onStartedAuthentication();
				AuthenticationResult authenticationResult = getAuthenticationResult(username, password);
				callback.onCompletedAuthentication(authenticationResult, getContext());
				
			}
		}
		
		private AuthenticationResult getAuthenticationResult(String username, String password)
		{
			if (username.equals("") && password.equals(""))
			{
				return LocalAuthenticationResult.notApplicable();
			}

			
			AuthenticationResult authenticationResult;
			try
			{
				authenticationResult = credentialExchange.checkPassword(
						username, password,  
						registrationFormForUnknown, enableAssociation, 
						callback.getTriggeringContext());
			} catch (AuthenticationException e)
			{
				log.info("Authentication error during password checking", e);
				authenticationResult = e.getResult();
			} catch (Exception e)
			{
				log.error("Runtime error during password checking", e);
				authenticationResult = LocalAuthenticationResult.failed(e);
			}
			if (authenticationResult.getStatus() == Status.success || 
					authenticationResult.getStatus() == Status.unknownRemotePrincipal)
			{
				clear();
			} else
			{
				setError();
			}
			return authenticationResult;
		}
		
		private void setError()
		{
			passwordField.setValue("");
		}
		
		private void showResetDialog()
		{
			PasswordCredentialResetController passReset = new PasswordCredentialResetController(msg,
					credentialExchange.getCredentialResetBackend(), credEditor,
					credResetLauncher.getConfiguration(), notificationPresenter);
			AuthenticationSubject subject = presetAuthenticatedIdentity == null ?
					null : AuthenticationSubject.identityBased(presetAuthenticatedIdentity);
			credResetLauncher.startCredentialReset(passReset.getInitialUI(Optional.ofNullable(subject)));
		}

		@Override
		public void focus()
		{
			if (presetAuthenticatedIdentity == null && usernameField.isEmpty())

				usernameField.focus();
			else
				passwordField.focus();
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

		public void setAuthenticatedIdentity(String authenticatedIdentity)
		{
			this.presetAuthenticatedIdentity = authenticatedIdentity;
			usernameField.setVisible(false);
		}

		public void clear()
		{
			passwordField.setValue("");
			usernameField.setValue("");
		}

		public void disablePasswordReset()
		{
			if (reset!= null)
				reset.setVisible(false);
		}

		public void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
			this.credResetLauncher = credResetLauncher;
		}
	}
	
	private class PasswordRetrievalUI implements VaadinAuthenticationUI
	{
		private final PasswordRetrievalComponent theComponent;


		public PasswordRetrievalUI(CredentialEditor credEditor)
		{
			this.theComponent = new PasswordRetrievalComponent(credEditor, notificationPresenter);
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
		 * Simple: there is only one authN option in this authenticator so we can return any constant id. 
		 */
		@Override
		public String getId()
		{
			return "password";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
			List<Identity> ids = authenticatedEntity.getIdentities();
			for (Identity id: ids)
				if (id.getTypeId().equals(UsernameIdentity.ID) || 
						id.getTypeId().equals(EmailIdentity.ID))
				{
					theComponent.setAuthenticatedIdentity(id.getValue());
					return;
				}
		}

		@Override
		public Set<String> getTags()
		{
			return Collections.emptySet();
		}

		@Override
		public void disableCredentialReset()
		{
			theComponent.disablePasswordReset();
		}

		@Override
		public void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
			theComponent.setCredentialResetLauncher(credResetLauncher);
		}
	}
	
	
	@org.springframework.stereotype.Component
	public static class Factory extends AbstractCredentialRetrievalFactory<PasswordRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<PasswordRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, PasswordExchange.ID);
		}
	}
}


