/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.callbacks.SandboxAuthnResultCallback;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.LogRecorder;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.CredentialReset1Dialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
public class PasswordRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordRetrieval.class);
	private UnityMessageSource msg;
	private PasswordExchange credentialExchange;
	private String name;
	private String registrationFormForUnknown;
	private CredentialEditorRegistry credEditorReg;

	public PasswordRetrieval(UnityMessageSource msg, CredentialEditorRegistry credEditorReg)
	{
		this.msg = msg;
		this.credEditorReg = credEditorReg;
	}

	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", name);
		root.put("registrationFormForUnknown", registrationFormForUnknown);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize web-based password retrieval configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			name = root.get("name").asText();
			JsonNode formNode = root.get("registrationFormForUnknown");
			if (formNode != null && !formNode.isNull())
				registrationFormForUnknown = formNode.asText();
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based password retrieval can not be parsed", e);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (PasswordExchange) e;
	}


	@Override
	public VaadinAuthenticationUI createUIInstance()
	{
		return new PasswordRetrievalUI(credEditorReg.getEditor(PasswordVerificatorFactory.NAME));
	}


	private class PasswordRetrievalUI implements VaadinAuthenticationUI
	{
		private UsernameProvider usernameProvider;
		private PasswordField passwordField;
		private CredentialEditor credEditor;
		private AuthenticationResultCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;

		public PasswordRetrievalUI(CredentialEditor credEditor)
		{
			this.credEditor = credEditor;
		}

		@Override
		public boolean needsCommonUsernameComponent()
		{
			return true;
		}

		@Override
		public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
		{
			this.callback = callback;
		}

		@Override
		public Component getComponent()
		{
			VerticalLayout ret = new VerticalLayout();
			ret.setSpacing(true);
			String label = name.trim().equals("") ? msg.getMessage("WebPasswordRetrieval.password") : name;
			passwordField = new PasswordField(label);
			passwordField.setId("WebPasswordRetrieval.password");
			ret.addComponent(passwordField);

			if (credentialExchange.getCredentialResetBackend().getSettings().isEnabled())
			{
				Button reset = new Button(msg.getMessage("WebPasswordRetrieval.forgottenPassword"));
				reset.setStyleName(Reindeer.BUTTON_LINK);
				ret.addComponent(reset);
				ret.setComponentAlignment(reset, Alignment.TOP_RIGHT);
				reset.addClickListener(new ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						showResetDialog();
					}
				});
			}

			return ret;
		}

		@Override
		public void setUsernameCallback(UsernameProvider usernameCallback)
		{
			this.usernameProvider = usernameCallback;
		}

		@Override
		public void triggerAuthentication()
		{
			String username = usernameProvider.getUsername();
			String password = passwordField.getValue();
			if (username.equals("") && password.equals(""))
			{
				passwordField.setComponentError(new UserError(
						msg.getMessage("WebPasswordRetrieval.noPassword")));
			}
			
			if (sandboxCallback != null)
			{
				if (sandboxCallback.validateProfile())
				{
					LogRecorder logRecorder = new LogRecorder();
					logRecorder.startLogRecording();
					
					AuthenticationResult authnResult = getAuthenticationResult(username, password);
					sandboxCallback.handleProfileValidation(authnResult, logRecorder.getCapturedLogs());
					
					logRecorder.stopLogRecording();
					
				} else
				{
					handleSandboxAuthn(username, password);
				}
			} else
			{
				callback.setAuthenticationResult(getAuthenticationResult(username, password));
			}
		}
		

		private void handleSandboxAuthn(String username, String password) 
		{
			if (username.equals("") && password.equals(""))
			{
				return;
			}
			try 
			{
				RemotelyAuthenticatedInput input = credentialExchange.getRemotelyAuthenticatedInput(
						username, password);
				sandboxCallback.handleAuthnInput(input);
			} catch (AuthenticationException e) 
			{
				sandboxCallback.handleAuthnError(e);
			}
		}

		private AuthenticationResult getAuthenticationResult(String username, String password)
		{
			if (username.equals("") && password.equals(""))
			{
				return new AuthenticationResult(Status.notApplicable, null);
			}
			try
			{
				AuthenticationResult authenticationResult = credentialExchange.checkPassword(username, password);
				if (authenticationResult.getStatus() == Status.success)
					passwordField.setComponentError(null);
				else if (authenticationResult.getStatus() == Status.unknownRemotePrincipal && 
						registrationFormForUnknown != null) 
				{
					authenticationResult.setFormForUnknownPrincipal(registrationFormForUnknown);
					passwordField.setValue("");
				} else
				{
					passwordField.setComponentError(new UserError(
							msg.getMessage("WebPasswordRetrieval.wrongPassword")));
					passwordField.setValue("");
				}
				return authenticationResult;
			} catch (Exception e)
			{
				if (!(e instanceof IllegalCredentialException) && 
						!(e instanceof IllegalIdentityValueException))
					log.warn("Password verificator has thrown an exception", e);
				passwordField.setComponentError(new UserError(
						msg.getMessage("WebPasswordRetrieval.wrongPassword")));
				passwordField.setValue("");
				return new AuthenticationResult(Status.deny, null);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getLabel()
		{
			return name;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Resource getImage()
		{
			return null;
		}

		private void showResetDialog()
		{
			CredentialReset1Dialog dialog = new CredentialReset1Dialog(msg, 
					credentialExchange.getCredentialResetBackend(), credEditor);
			dialog.show();
		}

		@Override
		public void cancelAuthentication()
		{
			//do nothing
		}

		@Override
		public void clear()
		{
			passwordField.setValue("");
		}

		@Override
		public void refresh(VaadinRequest request) 
		{
			//nop
		}

		@Override
		public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback) 
		{
			sandboxCallback = callback;
		}
	}
}










