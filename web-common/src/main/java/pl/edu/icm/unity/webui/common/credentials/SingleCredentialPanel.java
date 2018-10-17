/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler.AuthnResult;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialsPanel.Callback;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;

/**
 * Single credential editor with credential extra info
 * @author P.Piernik
 *
 */
public class SingleCredentialPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleCredentialPanel.class);
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	private CredentialManagement credMan;
	private UnityMessageSource msg;
	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private final boolean simpleMode;
	private final boolean showButtons;
	private HtmlConfigurableLabel credentialName;
	private VerticalLayout credentialStatus;
	private Button update;
	private Button clear;
	private Button invalidate;
	private CredentialEditor credEditor;
	private ComponentsContainer credEditorComp;
	private CredentialDefinition toEdit;
	private LocalCredentialState credentialState;
	private Callback callback;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	
	
	public SingleCredentialPanel(AdditionalAuthnHandler additionalAuthnHandler, UnityMessageSource msg, long entityId,
			EntityCredentialManagement ecredMan, CredentialManagement credMan,
			EntityManagement entityMan, CredentialEditorRegistry credEditorReg,
			CredentialDefinition toEdit, boolean simpleMode, boolean showButtons, Callback callback)
			throws Exception
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.ecredMan = ecredMan;
		this.credMan = credMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.simpleMode = simpleMode;
		this.showButtons = showButtons;
		this.toEdit = toEdit;
		this.callback = callback;
		loadEntity(new EntityParam(entityId));
		init();
	}

	private void init() throws Exception
	{
		credentialName = new HtmlConfigurableLabel();
		credentialName.setCaption(msg.getMessage("CredentialChangeDialog.credentialName"));
		credentialStatus = new VerticalLayout();
		credentialStatus.setMargin(false);
		credentialStatus.setCaption(
				msg.getMessage("CredentialChangeDialog.credentialStateInfo"));

		credEditor = credEditorReg.getEditor(toEdit.getTypeId());

		credEditorComp = credEditor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(toEdit.getConfiguration())
				.withRequired(true)
				.withEntityId(entityId)
				.withAdminMode(!simpleMode)
				.build());
		
		clear = new Button(msg.getMessage("CredentialChangeDialog.clear"));
		clear.setIcon(Images.undeploy.getResource());
		clear.addClickListener(e -> {
			changeCredentialStatus(LocalCredentialState.notSet);
			if (callback != null)
				callback.refresh();
		});

		invalidate = new Button(msg.getMessage("CredentialChangeDialog.invalidate"));
		invalidate.setIcon(Images.warn.getResource());
		invalidate.addClickListener(ne -> {
			changeCredentialStatus(LocalCredentialState.outdated);
				callback.refresh();
		});

		update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.setIcon(Images.save.getResource());
		update.addClickListener(e -> {
			boolean updated = updateCredential(true);
			if (updated && callback != null)
				callback.refresh();
		});

		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setSpacing(true);
		buttonsBar.setMargin(false);

		if (showButtons)
		{
			if (!simpleMode)
			{
				buttonsBar.addComponent(clear);
				if (isSupportInvalidate(toEdit.getTypeId()))
				{
					buttonsBar.addComponent(invalidate);
				}
			}
			buttonsBar.addComponent(update);
		}

		FormLayout fl = new CompactFormLayout(credentialName, credentialStatus);
		fl.setMargin(true);
		addComponent(fl);
		if (!isEmptyEditor())
		{
			fl.addComponent(new Label());
			fl.addComponents(credEditorComp.getComponents());
			addComponent(buttonsBar);
		}

		setSpacing(true);
		setMargin(false);
		updateCredentialStatus();
	}

	public AbstractField<?> getFocussedComponent()
	{
		for (Component component: credEditorComp.getComponents())
			if (component instanceof AbstractField<?>)
				return (AbstractField<?>) component;
		return null;
	}
	
	public boolean isChanged()
	{
		return changed;
	}

	public boolean isEmptyEditor()
	{
		return credEditorComp.getComponents().length == 0;
	}
	
	private String getStatusIcon()
	{
		if (credentialState.equals(LocalCredentialState.correct))
			return Images.ok.getHtml();
		else if (credentialState.equals(LocalCredentialState.notSet))
			return Images.undeploy.getHtml();
		else
			return Images.warn.getHtml();
	}
	
	public LocalCredentialState getCredentialState()
	{
		return credentialState;
	}
	
	private boolean isSupportInvalidate(String credType)
	{
		try
		{
			Collection<CredentialType> credentialTypes = credMan.getCredentialTypes();
			for (CredentialType type : credentialTypes)
			{
				if (type.getName().equals(credType))
				{
					return type.isSupportingInvalidation();
				}
			}
		} catch (EngineException e)
		{
			log.debug("Can not get credential type " + credType, e);
		}
		return false;
	}

	private void updateCredentialStatus()
	{

		String desc = toEdit.getDescription().getValue(msg);
		if (desc != null && !desc.isEmpty())
		{
			credentialName.setValue(desc);
		} else
		{
			credentialName.setValue(toEdit.getName());
		}

		Map<String, CredentialPublicInformation> s = entity.getCredentialInfo()
				.getCredentialsState();
		CredentialPublicInformation credPublicInfo = s.get(toEdit.getName());
		credentialState = credPublicInfo.getState();	
		credentialStatus.removeAllComponents();
		Label status = new Label(getStatusIcon() + " "
				+ msg.getMessage("CredentialStatus."
						+ credPublicInfo.getState().toString()));
		status.setContentMode(ContentMode.HTML);
		credentialStatus.addComponent(status);
		ComponentsContainer viewer = credEditor
				.getViewer(credPublicInfo.getExtraInformation());

		if (viewer == null)
		{
			credentialStatus.setVisible(false);
		} else
		{
			credentialStatus.addComponents(viewer.getComponents());
			credentialStatus.setVisible(true);
		}
		if (credPublicInfo.getState() == LocalCredentialState.notSet)
		{
			clear.setEnabled(false);
			invalidate.setEnabled(false);
		} else if (credPublicInfo.getState() == LocalCredentialState.outdated)
		{
			clear.setEnabled(true);
			invalidate.setEnabled(false);
		} else
		{
			clear.setEnabled(true);
			invalidate.setEnabled(true);
		}
	}

	public boolean updateCredential(boolean showSuccess)
	{
		String secrets;
		try
		{
			secrets = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			NotificationPopup.showError(msg
					.getMessage("CredentialChangeDialog.credentialUpdateError"),
					e.getMessage());
			return false;
		}
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			ecredMan.setEntityCredential(entityP, toEdit.getName(), secrets);
		} catch (IllegalCredentialException e)
		{
			credEditor.setCredentialError(e);
			return false;
		} catch (AdditionalAuthenticationRequiredException additionalAuthn)
		{
			additionalAuthnHandler.handleAdditionalAuthenticationException(additionalAuthn, 
					msg.getMessage("CredentialChangeDialog.additionalAuthnRequired"), 
					msg.getMessage("CredentialChangeDialog.additionalAuthnRequiredInfo"),
					this::onAdditionalAuthnForUpdateCredential);
			return false;
		} catch (AdditionalAuthenticationMisconfiguredException misconfigured)
		{
			NotificationPopup.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
					msg.getMessage("AdditionalAuthenticationMisconfiguredError"));
			return false;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e);
			return false;
		}
		credEditor.setCredentialError(null);
		if (showSuccess)
			NotificationPopup.showSuccess(msg.getMessage("CredentialChangeDialog.credentialUpdated"), "");
		changed = true;
		loadEntity(entityP);
		updateCredentialStatus();
		return true;
	}
	
	private void onAdditionalAuthnForUpdateCredential(AuthnResult result)
	{
		if (result == AuthnResult.SUCCESS)
		{
			updateCredential(true);
			if (callback != null)
				callback.refresh();
		} else if (result == AuthnResult.ERROR)
		{
			NotificationPopup.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
					msg.getMessage("CredentialChangeDialog.additionalAuthnFailed"));
			credEditor.setCredentialError(null);
		} else 
		{
			credEditor.setCredentialError(null);
		}
	}

	private void changeCredentialStatus(LocalCredentialState desiredState)
	{
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			ecredMan.setEntityCredentialStatus(entityP, toEdit.getName(), desiredState);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e);
			return;
		}
		loadEntity(entityP);
		updateCredentialStatus();
	}
	
	private void loadEntity(EntityParam entityP)
	{
		try
		{
			entity = entityMan.getEntity(entityP);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("CredentialChangeDialog.entityRefreshError"),
					e);
		}
	}

}
