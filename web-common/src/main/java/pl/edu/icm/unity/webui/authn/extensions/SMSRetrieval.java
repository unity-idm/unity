/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.extensions;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.stdext.credential.SMSExchange;
import pl.edu.icm.unity.stdext.credential.SMSVerificator;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.sms.SMSCredentialReset1Dialog;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Retrieves sms code using a Vaadin widget.
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class SMSRetrieval extends AbstractCredentialRetrieval<SMSExchange> implements VaadinAuthentication
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, SMSRetrieval.class);
	public static final String NAME = "web-sms";
	public static final String DESC = "WebSMSRetrievalFactory.desc";
	
	private UnityMessageSource msg;
	private I18nString name;
	private String logoURL;
	private CredentialEditorRegistry credEditorReg;
	
	@Autowired
	public SMSRetrieval(UnityMessageSource msg, CredentialEditorRegistry credEditorReg)
	{	
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.set("i18nName", I18nStringJsonUtil.toJson(name));
		if (logoURL != null)
			root.put("logoURL", logoURL);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize web-based SMS retrieval configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			name = I18nStringJsonUtil.fromJson(root.get("i18nName"), root.get("name"));
			if (name.isEmpty())
				name = new I18nString("WebSMSRetrieval.title", msg);
			JsonNode logoNode = root.get("logoURL");
			if (logoNode != null && !logoNode.isNull())
				logoURL = logoNode.asText();
			if (logoURL != null && !logoURL.isEmpty())
				ImageUtils.getLogoResource(logoURL);
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based SMS retrieval can not be parsed", e);
		}
	}
	
	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance()
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new SMSRetrievalUI(credEditorReg.getEditor(SMSVerificator.NAME)));
	}

	
	private class SMSRetrievalComponent extends CustomComponent implements Focusable
	{
		private CredentialEditor credEditor;
		private AuthenticationResultCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;
		private TextField usernameField;
		private HtmlLabel usernameLabel;
		private TextField answerField;
		private int tabIndex;
		private SMSCode sentCode = null;
		private Button sendCodeButton;
		private Button resetButton;
		private String username;
		private CaptchaComponent capcha;
		private VerticalLayout capchaComponent;
		private Label capchaInfoLabel;
		private VerticalLayout mainLayout;
		
		public SMSRetrievalComponent(CredentialEditor credEditor)
		{
			this.credEditor = credEditor;
			initUI();
		}

		private void initUI()
		{
			mainLayout = new VerticalLayout();
			mainLayout.setMargin(false);

			usernameField = new TextField(msg.getMessage("AuthenticationUI.username"));
			usernameField.setId("AuthenticationUI.username");
			mainLayout.addComponent(usernameField);
			mainLayout.setComponentAlignment(usernameField, Alignment.MIDDLE_CENTER);

			usernameLabel = new HtmlLabel(msg);
			mainLayout.addComponent(usernameLabel);
			mainLayout.setComponentAlignment(usernameLabel, Alignment.MIDDLE_CENTER);
			usernameLabel.setVisible(false);

			capcha = new CaptchaComponent(msg);
			capchaInfoLabel = new Label();
			capchaComponent = new VerticalLayout();
			capchaComponent.setMargin(false);
			capchaComponent.addComponent(capchaInfoLabel);
			capchaComponent.setComponentAlignment(capchaInfoLabel,
					Alignment.MIDDLE_CENTER);
			Component rCapchaComponent = capcha.getAsComponent(Alignment.MIDDLE_CENTER);
			capchaComponent.addComponent(rCapchaComponent);
			capchaComponent.setComponentAlignment(rCapchaComponent,
					Alignment.MIDDLE_CENTER);
			mainLayout.addComponent(capchaComponent);
			capchaComponent.setVisible(false);

			sendCodeButton = new Button(msg.getMessage("WebSMSRetrieval.sendCode"));
			sendCodeButton.setIcon(Images.mobile.getResource());
			sendCodeButton.addClickListener(e -> {
				if (username == null)
					username = usernameField.getValue();
				sendCode();
			});
			mainLayout.addComponent(sendCodeButton);
			mainLayout.setComponentAlignment(sendCodeButton, Alignment.MIDDLE_CENTER);

			resetButton = new Button(msg.getMessage("WebSMSRetrieval.reset"));
			resetButton.setIcon(Images.reject.getResource());
			resetButton.addClickListener(e -> {
				resetSentCode();
			});
			resetButton.setVisible(false);

			mainLayout.addComponent(resetButton);
			mainLayout.setComponentAlignment(resetButton, Alignment.MIDDLE_CENTER);

			answerField = new TextField();
			answerField.setCaption(msg.getMessage("WebSMSRetrieval.code"));
			answerField.setEnabled(false);
			mainLayout.addComponent(answerField);
			mainLayout.setComponentAlignment(answerField, Alignment.MIDDLE_CENTER);

			SMSCredentialRecoverySettings settings = new SMSCredentialRecoverySettings(
					JsonUtil.parse(credentialExchange
							.getSMSCredentialResetBackend()
							.getSettings()));

			if (settings.isEnabled())
			{
				Button lostPhone = new Button(
						msg.getMessage("WebSMSRetrieval.lostPhone"));
				lostPhone.setStyleName(Styles.vButtonLink.toString());
				mainLayout.addComponent(lostPhone);
				mainLayout.setComponentAlignment(lostPhone, Alignment.TOP_RIGHT);
				lostPhone.addClickListener(new ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						showResetDialog();
					}
				});
			}
			setCompositionRoot(mainLayout);
		}

		private void resetSentCode()
		{
			sendCodeButton.setVisible(true);
			resetButton.setVisible(false);
			capchaComponent.setVisible(false);
			usernameField.setVisible(true);
			usernameLabel.setVisible(false);
			usernameLabel.resetValue();
			answerField.setValue("");
			answerField.setEnabled(false);
			username = null;
			sentCode = null;
		}

		private void sendCode()
		{
			boolean force = false;
			
			if (username == null || username.equals(""))
			{
				usernameField.setComponentError(new UserError(
						msg.getMessage("WebSMSRetrieval.noUser")));
				return;
			}
		
			answerField.setComponentError(null);
			usernameField.setComponentError(null);
			usernameField.setVisible(false);
		
			usernameLabel.setVisible(true);
			resetButton.setVisible(true);			
			
			
			if (credentialExchange.isAuthSMSLimitExceeded(username)
					&& !capchaComponent.isVisible())
			{
				capchaInfoLabel.setValue(msg.getMessage(
						"WebSMSRetrieval.sentCodeLimit"));
				capchaComponent.setVisible(true);
				capcha.resetFull();
				usernameLabel.setHtmlValue("WebSMSRetrieval.usernameLabel",
						username);
				sendCodeButton.setVisible(true);
				log.debug("Too many authn sms code sent to the user, turn on capcha");
				return;
			}
					
			if (capchaComponent.isVisible())
			{
				try
				{
					capcha.verify();
					force = true;
				} catch (WrongArgumentException e)
				{
					return;
				}
			}
			
			try
			{
				sentCode = credentialExchange.sendCode(username, force);
				
			} catch (EngineException e)
			{
				log.debug("Cannot send authn sms code", e);
				usernameField.setComponentError(new UserError(
						msg.getMessage("WebSMSRetrieval.cannotSendSMS", username)));
				return;
			}
			
			usernameLabel.setHtmlValue("WebSMSRetrieval.usernameLabelCodeSent",
					username);
			capcha.reset();
			answerField.setEnabled(true);		
			capchaComponent.setVisible(false);
			sendCodeButton.setVisible(false);
		
		}

		public void triggerAuthentication()
		{

			if (username == null || username.equals(""))
			{
				setAuthenticationResult(new AuthenticationResult(
						Status.notApplicable, null));
				return;
			}
			setAuthenticationResult(credentialExchange.verifyCode(sentCode,
					answerField.getValue(), username, sandboxCallback));
		}

		private void setAuthenticationResult(AuthenticationResult authenticationResult)
		{
			if (authenticationResult.getStatus() == Status.success
					|| authenticationResult
							.getStatus() == Status.unknownRemotePrincipal)
			{
				clear();
			} else
			{
				setError();
			}
			callback.setAuthenticationResult(authenticationResult);
		}

		private void setError()
		{
			resetSentCode();
			String msgErr = msg.getMessage("WebSMSRetrieval.wrongCode");
			usernameField.setComponentError(new UserError(msgErr));
			usernameField.setValue("");
			answerField.setComponentError(new UserError(msgErr));
			answerField.setValue("");

		}

		private void showResetDialog()
		{
			SMSCredentialReset1Dialog dialog = new SMSCredentialReset1Dialog(msg,
					credentialExchange.getSMSCredentialResetBackend(),
					credEditor);
			dialog.show();
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

		public void setCallback(AuthenticationResultCallback callback)
		{
			this.callback = callback;
		}

		public void setSandboxCallback(SandboxAuthnResultCallback sandboxCallback)
		{
			this.sandboxCallback = sandboxCallback;
		}

		public void setAuthenticatedIdentity(String authenticatedIdentity)
		{
			this.username = authenticatedIdentity;
			sendCodeButton.setVisible(false);
			mainLayout.removeComponent(usernameField);
			mainLayout.removeComponent(resetButton);
			mainLayout.removeComponent(usernameLabel);
			sendCode();
		}

		public void clear()
		{
			resetSentCode();
			usernameField.setValue("");
			usernameField.setComponentError(null);
			answerField.setValue("");
			answerField.setComponentError(null);
						
			
		}
	}

	private class SMSRetrievalUI implements VaadinAuthenticationUI
	{
		private SMSRetrievalComponent theComponent;

		public SMSRetrievalUI(CredentialEditor credEditor)
		{
			this.theComponent = new SMSRetrievalComponent(credEditor);
		}

		@Override
		public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
		{
			theComponent.setCallback(callback);
		}

		@Override
		public Component getComponent()
		{
			return theComponent;
		}

		@Override
		public void triggerAuthentication()
		{
			theComponent.triggerAuthentication();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getLabel()
		{
			return name.getValue(msg);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Resource getImage()
		{
			if (logoURL == null)
				return null;
			if ("".equals(logoURL))
				return Images.mobile_sms.getResource();
			else
			{
				try
				{
					return ImageUtils.getLogoResource(logoURL);
				} catch (MalformedURLException e)
				{
					log.error("Can't load logo", e);
					return null;
				}
			}
		}

		@Override
		public void cancelAuthentication()
		{
			// do nothing
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
		public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback)
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
			return "sms";
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
	}

	@org.springframework.stereotype.Component
	public static class Factory extends AbstractCredentialRetrievalFactory<SMSRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<SMSRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, SMSExchange.class);
		}
	}
}
