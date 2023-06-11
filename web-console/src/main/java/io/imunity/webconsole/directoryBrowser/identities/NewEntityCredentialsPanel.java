/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.directoryBrowser.identities.SingleCredentialPanel.ObtainedCredential;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistryV8;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Editing of credentials of an entity which does not exist yet.
 */
@PrototypeComponent
class NewEntityCredentialsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, NewEntityCredentialsPanel.class);
	private CredentialManagement credMan;
	private CredentialRequirementManagement credReqMan;
	private CredentialEditorRegistryV8 credEditorReg;
	private MessageSource msg;
	
	private Map<String, CredentialDefinition> credentials;
	private List<SingleCredentialPanel> panels;
	private String credReqId;

	@Autowired
	private NewEntityCredentialsPanel(MessageSource msg, 
			CredentialManagement credMan, 
			CredentialRequirementManagement credReqMan,
			CredentialEditorRegistryV8 credEditorReg)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
	}
	
	@org.springframework.stereotype.Component
	public static class CredentialsPanelFactory
	{
		private ObjectFactory<NewEntityCredentialsPanel> factory;

		@Autowired
		CredentialsPanelFactory(ObjectFactory<NewEntityCredentialsPanel> factory)
		{
			this.factory = factory;
		}
		
		public NewEntityCredentialsPanel getInstance(String credReqId)
		{
			NewEntityCredentialsPanel panel = factory.getObject();
			panel.init(credReqId);
			return panel;
		}
	}

	List<ObtainedCredential> getCredentials()
	{
		return panels.stream().map(panel -> panel.getCredential()).filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}
	
	private void init(String credReqId)
	{
		this.credReqId = credReqId;
		addComponent(HtmlTag.horizontalLine());
		loadCredentials();
		if (credentials.size() == 0)
		{
			addComponent(new Label(
					msg.getMessage("CredentialChangeDialog.noCredentials")));
			return;
		}
		panels = new ArrayList<>();	
		
		for (CredentialDefinition credDef : credentials.values())
		{
			SingleCredentialPanel panel = new SingleCredentialPanel(msg, credMan, credEditorReg, credDef);
			if (!panel.isEmptyEditor())
				panels.add(panel);
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
		setMargin(false);
		setSizeFull();
	}

	
	private void loadCredentials()
	{
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
}
