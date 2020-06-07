/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.fido.service;

import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import eu.unicore.util.configuration.ConfigurationException;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.fido.FidoManagement;
import pl.edu.icm.unity.fido.component.FidoComponent;
import pl.edu.icm.unity.fido.credential.FidoCredentialVerificator;
import pl.edu.icm.unity.fido.credential.FidoExchange;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.extensions.SMSRetrievalProperties;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

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

	private UnityMessageSource msg;
	private I18nString name;
	private CredentialEditorRegistry credEditorReg;
	private String configuration;
	private FidoManagement fidoManagement;

	@Autowired
	public FidoRetrieval(UnityMessageSource msg, CredentialEditorRegistry credEditorReg, FidoManagement fidoManagement)
	{	
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
		this.fidoManagement = fidoManagement;
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
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context)
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new FidoRetrievalUI(credEditorReg.getEditor(FidoCredentialVerificator.NAME)));
	}

	@Override
	public boolean supportsGrid()
	{
		return false;
	}
	
	private class FidoRetrievalComponent extends CustomComponent implements Focusable
	{
		private AuthenticationCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;
		private TextField usernameField;
		private int tabIndex;
		private String username;
		private VerticalLayout mainLayout;
		private Button authenticateButton;
		private FidoComponent fidoComponent;
		
		public FidoRetrievalComponent(CredentialEditor credEditor)
		{
			initUI();
		}

		private void initUI()
		{
			mainLayout = new VerticalLayout();
			mainLayout.setMargin(false);

			fidoComponent = FidoComponent.builder(fidoManagement ,msg)
					.showSuccessNotification(false)
					.authenticationResultListener(this::setAuthenticationResult)
					.build();
			mainLayout.addComponent(fidoComponent);

			usernameField = new TextField();
			usernameField.setWidth(100, Unit.PERCENTAGE);
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			usernameField.addStyleName("u-authnTextField");
			usernameField.addStyleName("u-passwordUsernameFieldd");
			mainLayout.addComponent(usernameField);

			authenticateButton = new Button(msg.getMessage("Fido.WebRetrieval.signIn"));
			authenticateButton.addClickListener(event -> triggerAuthentication());
			authenticateButton.addStyleName(Styles.signInButton.toString());
			authenticateButton.addStyleName("u-passwordSignInButton");
			authenticateButton.setIcon(Images.fido.getResource());
			mainLayout.addComponent(authenticateButton);

			setCompositionRoot(mainLayout);
		}

		private void triggerAuthentication()
		{
			username = usernameField.getValue();
			if (username == null || username.equals(""))
			{
				setAuthenticationResult(new AuthenticationResult(
						Status.notApplicable, null));
				return;
			}
			fidoComponent.invokeAuthentication(username);
		}

		private void setAuthenticationResult(AuthenticationResult authenticationResult)
		{
			log.debug("Enter setAuthenticationResult: {}", authenticationResult);
			if (authenticationResult.getStatus() == Status.success)
			{
				clear();
				setEnabled(false);
				callback.onCompletedAuthentication(authenticationResult);
			} else if (authenticationResult.getStatus() == Status.unknownRemotePrincipal)
			{
				clear();
				callback.onCompletedAuthentication(authenticationResult);
			} else
			{
				setError();
				usernameField.focus();
				String msgErr = msg.getMessage("Fido.WebRetrieval.wrongUsername");
				callback.onFailedAuthentication(authenticationResult, msgErr, Optional.empty());
			}
		}
		
		private void setError()
		{
			usernameField.setValue("");
		}

		@Override
		public void focus()
		{
			if (username == null)
				usernameField.focus();
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

		public void setSandboxCallback(SandboxAuthnResultCallback sandboxCallback)
		{
			this.sandboxCallback = sandboxCallback;
		}

		void setAuthenticatedIdentity(String authenticatedIdentity)
		{
			this.username = authenticatedIdentity;
			mainLayout.removeComponent(usernameField);
		}

		private void clear()
		{
			usernameField.setValue("");
		}

		void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
			// FIXME is it applicable?
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
			theComponent.setCredentialResetLauncher(credResetLauncher);
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
			return Images.fido.getResource();
		}

		@Override
		public void clear()
		{
			theComponent.clear();
		}

		@Override
		public void refresh(VaadinRequest request)
		{
			// nop
		}

		@Override
		public void setSandboxAuthnCallback(SandboxAuthnResultCallback callback)
		{
			theComponent.setSandboxCallback(callback);
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
			List<Identity> ids = authenticatedEntity.getIdentities();
			for (Identity id : ids)
				if (id.getTypeId().equals(UsernameIdentity.ID))
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
