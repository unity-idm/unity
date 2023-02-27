/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Resource;
import com.vaadin.ui.*;
import com.vaadin.ui.Component.Focusable;
import eu.unicore.util.configuration.ConfigurationException;
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
import pl.edu.icm.unity.webui.authn.AuthNGridTextWrapper;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import java.io.StringReader;
import java.util.*;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class PasswordRetrieval extends AbstractCredentialRetrieval<PasswordExchange> implements VaadinAuthentication
{
	public static final String NAME = "web-password";
	public static final String DESC = "WebPasswordRetrievalFactory.desc";
	
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordRetrieval.class);
	private MessageSource msg;
	private I18nString name;
	private String registrationFormForUnknown;
	private boolean enableAssociation;
	private CredentialEditorRegistry credEditorReg;
	private String configuration;

	@Autowired
	public PasswordRetrieval(MessageSource msg, CredentialEditorRegistry credEditorReg)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
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
	
	private class PasswordRetrievalComponent extends CustomComponent implements Focusable
	{
		private CredentialEditor credEditor;
		private AuthenticationCallback callback;
		private String presetAuthenticatedIdentity;
		
		private TextField usernameField;
		private PasswordField passwordField;
		private int tabIndex;
		private Button reset;
		private CredentialResetLauncher credResetLauncher;

		public PasswordRetrievalComponent(CredentialEditor credEditor)
		{
			this.credEditor = credEditor;
			initUI();
		}

		private void initUI()
		{
			VerticalLayout ret = new VerticalLayout();
			ret.setMargin(false);
			
			usernameField = new TextField();
			usernameField.setWidth(100, Unit.PERCENTAGE);
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			usernameField.addStyleName("u-authnTextField");
			usernameField.addStyleName("u-passwordUsernameField");
			ret.addComponent(usernameField);
			
			String label = name.getValue(msg);
			passwordField = new PasswordField();
			passwordField.setWidth(100, Unit.PERCENTAGE);
			passwordField.setPlaceholder(label);
			passwordField.addStyleName("u-authnTextField");
			passwordField.addStyleName("u-passwordField");
			ret.addComponent(passwordField);
			
			
			Button authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
			authenticateButton.addStyleName(Styles.signInButton.toString());
			authenticateButton.addStyleName("u-passwordSignInButton");
			authenticateButton.addClickListener(event -> triggerAuthentication());
			ret.addComponent(authenticateButton);

			passwordField.addFocusListener(e -> authenticateButton.setClickShortcut(KeyCode.ENTER));
			passwordField.addBlurListener(e -> authenticateButton.removeClickShortcut());

			PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings(
					JsonUtil.parse(credentialExchange
							.getCredentialResetBackend()
							.getSettings()));
			if (settings.isEnabled())
			{
				reset = new Button(msg.getMessage("WebPasswordRetrieval.forgottenPassword"));
				reset.setStyleName(Styles.vButtonLink.toString());
				AuthNGridTextWrapper resetWrapper = new AuthNGridTextWrapper(reset, Alignment.TOP_RIGHT);
				resetWrapper.addStyleName("u-authn-forgotPassword");
				ret.addComponent(resetWrapper);
				reset.addClickListener(event -> showResetDialog());
			}
			
			setCompositionRoot(ret);
		}

		private void triggerAuthentication()
		{
			String username = presetAuthenticatedIdentity == null ? usernameField.getValue() : 
				presetAuthenticatedIdentity;
			String password = passwordField.getValue();

			if (password.equals(""))
			{
				NotificationPopup.showErrorAutoClosing(msg.getMessage("AuthenticationUI.authnErrorTitle"), 
						msg.getMessage("WebPasswordRetrieval.noPassword"));
			} else if (username.equals(""))
			{
				NotificationPopup.showErrorAutoClosing(msg.getMessage("AuthenticationUI.authnErrorTitle"), 
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
					credResetLauncher.getConfiguration());
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
		private PasswordRetrievalComponent theComponent;


		public PasswordRetrievalUI(CredentialEditor credEditor)
		{
			this.theComponent = new PasswordRetrievalComponent(credEditor);
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
		public Resource getImage()
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


