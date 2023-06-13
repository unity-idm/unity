/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.CredentialType;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandlerV8;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandlerV8.AuthnResult;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;

/**
 * Single credential editor with credential extra info
 * @author P.Piernik
 */
public class SingleCredentialPanel extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleCredentialPanel.class);
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistryV8 credEditorReg;
	private CredentialManagement credMan;
	private MessageSource msg;
	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private final boolean enableAdminOptions;
	private HtmlConfigurableLabel credentialName;
	private Label credentialStatus;
	private VerticalLayout credentialExtraInfo;
	private Button edit;
	private Button clear;
	private Button invalidate;
	private CredentialEditor credEditor;
	private SingleCredentialEditComponent credEditorPanel;
	private CredentialDefinition toEdit;
	private LocalCredentialState credentialState;
	private final AdditionalAuthnHandlerV8 additionalAuthnHandler;
	private Component actionsBar;
	
	
	public SingleCredentialPanel(AdditionalAuthnHandlerV8 additionalAuthnHandler, MessageSource msg, long entityId,
	                             EntityCredentialManagement ecredMan, CredentialManagement credMan,
	                             EntityManagement entityMan, CredentialEditorRegistryV8 credEditorReg,
	                             CredentialDefinition toEdit, boolean enableAdminActions)
			throws Exception
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.ecredMan = ecredMan;
		this.credMan = credMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.enableAdminOptions = enableAdminActions;
		this.toEdit = toEdit;
		loadEntity(new EntityParam(entityId));
		init();
	}

	private void init() throws Exception
	{
		credentialName = new HtmlConfigurableLabel();
		credentialName.setCaption(msg.getMessage("CredentialChangeDialog.credentialName"));
		credentialStatus = new Label();
		credentialStatus.setContentMode(ContentMode.HTML);
		credentialStatus.setCaption(msg.getMessage("CredentialChangeDialog.credentialStateInfo"));
		
		credentialExtraInfo = new VerticalLayout();
		credentialExtraInfo.setMargin(false);
		
		credEditor = credEditorReg.getEditor(toEdit.getTypeId());
		
		ComponentsContainer editorComponents = credEditor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(toEdit.getConfiguration())
				.withRequired(true)
				.withEntityId(entityId)
				.withAdminMode(enableAdminOptions)
				.withCustomWidth(SingleCredentialEditComponent.WIDTH)
				.withCustomWidthUnit(Unit.EM)
				.withCredentialName(toEdit.getName())
				.build());
		credEditorPanel = new SingleCredentialEditComponent(msg, editorComponents, this::onCredentialUpdate, 
				this::hideEditor);
		credEditorPanel.setVisible(false);
		
		FormLayout fl = new CompactFormLayout(credentialName, credentialStatus, credentialExtraInfo, 
				credEditorPanel);
		actionsBar = createActionsBar();
		fl.addComponent(actionsBar);
		fl.setMargin(true);
		setCompositionRoot(fl);

		updateCredentialStatus();
	}
	
	private Component createActionsBar()
	{
		clear = new Button(msg.getMessage("CredentialChangeDialog.clear"));
		clear.addStyleName(Styles.vButtonLink.toString());
		clear.addClickListener(e -> clearCredential());

		invalidate = new Button(msg.getMessage("CredentialChangeDialog.invalidate"));
		invalidate.addStyleName(Styles.vButtonLink.toString());
		invalidate.addClickListener(ne -> changeCredentialStatusWithConfirm(LocalCredentialState.outdated));

		edit = new Button(msg.getMessage("CredentialChangeDialog.setup"));
		edit.addStyleName(Styles.vButtonLink.toString());
		edit.addClickListener(e -> switchEditorVisibility(true));
		
		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setSpacing(true);
		buttonsBar.setMargin(false);
		buttonsBar.addComponent(edit);
		if (enableAdminOptions)
		{
			buttonsBar.addComponent(clear);
			if (isInvalidationSupported(toEdit.getTypeId()))
				buttonsBar.addComponent(invalidate);
		}
		return buttonsBar;
	}
	
	boolean isChanged()
	{
		return changed;
	}

	boolean isEmptyEditor()
	{
		return credEditorPanel.isEmpty();
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
	
	LocalCredentialState getCredentialState()
	{
		return credentialState;
	}
	
	private boolean isInvalidationSupported(String credType)
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
			log.warn("Can not get credential type " + credType, e);
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
		credentialStatus.setValue(getStatusIcon() + " "
				+ msg.getMessage("CredentialStatus." + credPublicInfo.getState().toString()));
		
		Optional<Component> viewer = credEditor.getViewer(credPublicInfo.getExtraInformation());
		credentialExtraInfo.removeAllComponents();
		if (viewer.isPresent())
		{
			credentialExtraInfo.addComponent(viewer.get());
			credentialExtraInfo.setVisible(true);
		} else
		{
			credentialExtraInfo.setVisible(false);
		}
		
		updateEditCaption(credPublicInfo.getState());
		if (enableAdminOptions)
			updateAdminButtonsState(credPublicInfo.getState());
	}

	private void updateEditCaption(LocalCredentialState state)
	{
		String captionKey = state == LocalCredentialState.notSet ? "CredentialChangeDialog.setup" 
				: "CredentialChangeDialog.change";
		edit.setCaption(msg.getMessage(captionKey));
	}
	
	private void updateAdminButtonsState(LocalCredentialState state)
	{
		if (state == LocalCredentialState.notSet)
		{
			clear.setVisible(false);
			invalidate.setVisible(false);
		} else if (state == LocalCredentialState.outdated)
		{
			clear.setVisible(true);
			invalidate.setVisible(false);
		} else
		{
			clear.setVisible(true);
			invalidate.setVisible(true);
		}
	}

	private void hideEditor()
	{
		switchEditorVisibility(false);
	}

	private void switchEditorVisibility(boolean editorVisible)
	{
		if (credentialExtraInfo.getComponentCount() > 0)
			credentialExtraInfo.setVisible(!editorVisible);
		credentialStatus.setVisible(!editorVisible);
		actionsBar.setVisible(!editorVisible);
		credEditorPanel.setVisible(editorVisible);
		if (editorVisible)
			credEditorPanel.focusEditor();
	}

	private void clearCredential()
	{
		changeCredentialStatusWithConfirm(LocalCredentialState.notSet);
	}
	
	private void onCredentialUpdate()
	{
		boolean updated = updateCredential();
		if (updated)
			hideEditor();
	}
	
	private boolean updateCredential()
	{
		if (credEditor.isCredentialCleared()){
			clearCredential();
			return true;
		}

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
			updateCredential();
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

	
	private void changeCredentialStatusWithConfirm(LocalCredentialState desiredState)
	{
		new ConfirmDialog(msg, msg.getMessage("CredentialChangeDialog.confirmStateChangeTo." + desiredState,
				credentialName.getValue()), () -> changeCredentialStatus(desiredState)).show();
	}

	private void changeCredentialStatus(LocalCredentialState desiredState)
	{
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			ecredMan.setEntityCredentialStatus(entityP, toEdit.getName(), desiredState);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e);
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
