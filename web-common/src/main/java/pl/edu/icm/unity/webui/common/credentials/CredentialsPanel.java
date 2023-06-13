/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandlerV8;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Presents all allowed entity credentials with management options (e.g. to setup or change credential) 
 */
public class CredentialsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialsPanel.class);
	private CredentialManagement credMan;
	private CredentialRequirementManagement credReqMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistryV8 credEditorReg;
	private TokensManagement tokenMan;
	private MessageSource msg;
	private Entity entity;
	private final AdditionalAuthnHandlerV8 additionalAuthnHandler;
	private final long entityId;
	private final boolean enableAdminActions;
	private final boolean disable2ndFactorOptIn;
	
	private Map<String, CredentialDefinition> credentials;
	private List<SingleCredentialPanel> panels;
	private CheckBox userOptInCheckBox;

	
	/**
	 * @param disableAdminActions if true then admin-only action buttons (credential reset/outdate) are not shown.
	 */
	public CredentialsPanel(AdditionalAuthnHandlerV8 additionalAuthnHandler, MessageSource msg, long entityId, CredentialManagement credMan,
	                        EntityCredentialManagement ecredMan, EntityManagement entityMan,
	                        CredentialRequirementManagement credReqMan,
	                        CredentialEditorRegistryV8 credEditorReg, TokensManagement tokenMan,
	                        boolean disableAdminActions, boolean disable2ndFactorOptIn)
					throws Exception
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		this.disable2ndFactorOptIn = disable2ndFactorOptIn;
		this.enableAdminActions = !disableAdminActions;
		this.tokenMan = tokenMan;
		init();
	}
	
	
	private void init() throws Exception
	{
		if (!disable2ndFactorOptIn)
			add2ndFactorOptInCompnent();
		
		loadCredentials();
		if (credentials.size() == 0)
		{
			addComponent(new Label(msg.getMessage("CredentialChangeDialog.noCredentials")));
			return;
		}
		panels = new ArrayList<>();	
		
		for (CredentialDefinition credDef : credentials.values())
		{
			SingleCredentialPanel panel = new SingleCredentialPanel(additionalAuthnHandler, msg, entityId,
					ecredMan, credMan, entityMan, credEditorReg, credDef, enableAdminActions);
			if (!panel.isEmptyEditor())
			{
				panels.add(panel);
			}
		}
		
		int last = panels.size();
		int credSize = panels.size();
		for (SingleCredentialPanel panel: panels)
		{
			if (last > 0 && last < credSize)
				addComponent(HtmlTag.horizontalLine());
			addComponent(panel);
			last--;
		}
		
		addComponent(HtmlTag.horizontalLine());
		addComponent(getTrustedDevicesComponent());

		setSizeFull();
	}

	private void add2ndFactorOptInCompnent()
	{
		userOptInCheckBox = new CheckBox(msg.getMessage("CredentialChangeDialog.userMFAOptin"));
		userOptInCheckBox.setDescription(msg.getMessage("CredentialChangeDialog.userMFAOptinDesc"));
		FormLayout wrapper = new FormLayout();
		wrapper.setSpacing(false);
		wrapper.addComponent(userOptInCheckBox);
		addComponent(wrapper);
		addComponent(HtmlTag.horizontalLine());
		userOptInCheckBox.addValueChangeListener(e -> setUserMFAOptin(e.getValue()));
		userOptInCheckBox.setValue(getUserOptInAttribute());
	}
	
	private Component getTrustedDevicesComponent()
	{	
		TrustedDevicesComponent trustedDevicesComponent = new TrustedDevicesComponent(tokenMan, msg, entityId);
		trustedDevicesComponent.setVisible(false);		
		VerticalLayout trustedDevicesWrapper = new VerticalLayout();
		trustedDevicesWrapper.setMargin(false);
		trustedDevicesWrapper.setSpacing(true);
		
		Button removeTrustedMachines = new Button(msg.getMessage("CredentialChangeDialog.removeTrustedDevices"));
		removeTrustedMachines.addClickListener(e -> trustedDevicesComponent.removeAll());
		
		Button showHideTrustedMachines = new Button();
		showHideTrustedMachines.setDescription(msg.getMessage("CredentialChangeDialog.showTrustedDevices"));
		showHideTrustedMachines.addClickListener(e -> {
			if (trustedDevicesComponent.isVisible())
			{
				trustedDevicesComponent.setVisible(false);
				showHideTrustedMachines.setDescription(msg.getMessage("CredentialChangeDialog.showTrustedDevices"));
				showHideTrustedMachines.setIcon(Images.downArrow.getResource());
			}else
			{
				trustedDevicesComponent.setVisible(true);
				showHideTrustedMachines.setDescription(msg.getMessage("CredentialChangeDialog.hideTrustedDevices"));
				showHideTrustedMachines.setIcon(Images.upArrow.getResource());
			}
		});
		showHideTrustedMachines.setIcon(Images.downArrow.getResource());
		showHideTrustedMachines.setStyleName(Styles.vButtonLink.toString());
		showHideTrustedMachines.addStyleName(Styles.vButtonBorderless.toString());
		Label showInfo = new Label(msg.getMessage("TrustedDevicesComponent.caption"));
		HorizontalLayout showHide = new HorizontalLayout();
		showHide.setMargin(false);
		showHide.addComponents(showInfo, showHideTrustedMachines);
		
		
		trustedDevicesWrapper.addComponents(removeTrustedMachines, showHide, trustedDevicesComponent);
		return trustedDevicesWrapper;
	}
	
	private void setUserMFAOptin(Boolean value)
	{
		try
		{
			ecredMan.setUserMFAOptIn(new EntityParam(entityId), value);
		} catch (EngineException e)
		{
			log.warn("Can not set user MFA optin attribute", e);
			throw new InternalException(msg.getMessage(
					"CredentialChangeDialog.cantSetUserMFAOptin"), e);
		}
	}

	private boolean getUserOptInAttribute()
	{
		try
		{
			return ecredMan.getUserMFAOptIn(new EntityParam(entityId));
		} catch (EngineException e)
		{
			log.warn("Can not get user MFA optin attribute", e);
			throw new InternalException(msg.getMessage(
					"CredentialChangeDialog.cantGetUserMFAOptin"), e);
		}
	}
	

	public boolean isChanged()
	{	
		for (SingleCredentialPanel panel : panels)
			if (panel.isChanged())
				return true;
		
		return false;
	}
	
	public boolean isCredentialRequirementEmpty()
	{
		return credentials.isEmpty();
	}	
	
	private void loadCredentials() throws Exception
	{
		try
		{
			entity = entityMan.getEntity(new EntityParam(entityId));
		} catch (Exception e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.getEntityError"), e);
		}
		
		CredentialInfo ci = entity.getCredentialInfo();
		String credReqId = ci.getCredentialRequirementId();
		CredentialRequirements credReq = null;
		Collection<CredentialDefinition> allCreds = null;
		try
		{
			Collection<CredentialRequirements> allReqs = credReqMan.getCredentialRequirements();
			for (CredentialRequirements cr: allReqs)
				if (credReqId.equals(cr.getName()))
					credReq = cr;
			
		} catch (Exception e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredReqs"), e);
		}
		
		if (credReq == null)
		{
			log.fatal("Can not find credential requirement information, for the one set for the entity: " 
					+ credReqId);
			throw new InternalException(msg.getMessage("CredentialChangeDialog.noCredReqDef"));
		}
		
		try
		{
			allCreds = credMan.getCredentialDefinitions();
		} catch (EngineException e)
		{
			throw new InternalException(msg.getMessage("CredentialChangeDialog.cantGetCredDefs"), e);
		}
		
		credentials = new HashMap<>();
		Set<String> required = credReq.getRequiredCredentials();
		for (CredentialDefinition credential: allCreds)
		{
			if (required.contains(credential.getName()))
				credentials.put(credential.getName(), credential);
		}
	}
	
	public interface Callback 
	{
		public void refresh();
	}
	
}
