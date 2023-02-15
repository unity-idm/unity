/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.fido.web.v8;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.fido.FidoExchange;
import io.imunity.fido.component.FidoComponent;
import io.imunity.fido.service.FidoCredentialVerificator;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.extensions.SMSRetrievalProperties;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistryV8;

/**
 * Retrieves FIDO authentication input and validate FIDO authentication using FidoComponent.
 *
 * @author R.Ledzinski
 *
 */
@PrototypeComponent
public class FidoRetrieval extends AbstractCredentialRetrieval<FidoExchange> implements VaadinAuthentication
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, FidoRetrieval.class);
	public static final String NAME = "web-fido";
	public static final String DESC = "fido.desc";

	private MessageSource msg;
	private I18nString name;
	private CredentialEditorRegistryV8 credEditorReg;
	private String configuration;

	@Autowired
	public FidoRetrieval(MessageSource msg, CredentialEditorRegistryV8 credEditorReg)
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

	private class FidoRetrievalComponent extends CustomComponent implements Focusable
	{
		private AuthenticationCallback callback;
		private TextField usernameField;
		private int tabIndex;
		private VerticalLayout mainLayout;
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
			addStyleName("u-fidoRetrieval");
			mainLayout = new VerticalLayout();
			mainLayout.setMargin(false);
			mainLayout.setSpacing(false);
			mainLayout.addStyleName("u-fidoRetrievalLayout");
			
			fidoComponent = FidoComponent.builder(msg)
					.fidoExchange(credentialExchange)
					.showSuccessNotification(false)
					.authenticationResultListener(this::setAuthenticationResult)
					.build();
			fidoComponent.setHeight(0, Unit.PIXELS); // no UI but Vaadin allocates a lot of space for it
			mainLayout.addComponent(fidoComponent);

			visiblePart = new VerticalLayout();
			visiblePart.setMargin(false);
			usernameField = new TextField();
			usernameField.setWidth(100, Unit.PERCENTAGE);
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			usernameField.addStyleName("u-authnTextField");
			usernameField.addStyleName("u-passwordUsernameField");
			visiblePart.addComponent(usernameField);

			authenticateButton = new Button(msg.getMessage("Fido.WebRetrieval.signIn"));
			authenticateButton.addClickListener(event -> triggerAuthentication());
			authenticateButton.addStyleName(Styles.signInButton.toString());
			authenticateButton.addStyleName("u-passwordSignInButton");
			visiblePart.addComponent(authenticateButton);

			usernameField.addFocusListener(e -> authenticateButton.setClickShortcut(ShortcutAction.KeyCode.ENTER));
			usernameField.addBlurListener(e -> authenticateButton.removeClickShortcut());

			mainLayout.addComponent(visiblePart);
			setCompositionRoot(mainLayout);
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
				callback.onCompletedAuthentication(authenticationResult);
			} else if (authenticationResult.getStatus() == Status.notApplicable)
			{
				clear();
				usernameField.focus();
				AuthenticationResult exposedError = LocalAuthenticationResult.failed(
						new ResolvableError("Fido.invalidUsername"));
				callback.onCompletedAuthentication(exposedError);
			} else
			{
				usernameField.focus();
				AuthenticationResult exposedError = LocalAuthenticationResult.failed(
						new ResolvableError("Fido.authFailed"));
				callback.onCompletedAuthentication(exposedError);
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
			visiblePart.removeComponent(usernameField);
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
		public void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
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
