/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.*;
import java.util.stream.Collectors;

@PrototypeComponent
class NewEntityCredentialsPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, NewEntityCredentialsPanel.class);
	private final CredentialManagement credMan;
	private final CredentialRequirementManagement credReqMan;
	private final CredentialEditorRegistry credEditorReg;
	private final MessageSource msg;
	
	private Map<String, CredentialDefinition> credentials;
	private List<SingleCredentialPanel> panels;
	private String credReqId;

	@Autowired
	private NewEntityCredentialsPanel(MessageSource msg, 
			CredentialManagement credMan, 
			CredentialRequirementManagement credReqMan,
			CredentialEditorRegistry credEditorReg)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
	}
	
	@Component
	public static class CredentialsPanelFactory
	{
		private final ObjectFactory<NewEntityCredentialsPanel> factory;

		CredentialsPanelFactory(ObjectFactory<NewEntityCredentialsPanel> factory)
		{
			this.factory = factory;
		}
		
		NewEntityCredentialsPanel getInstance(String credReqId)
		{
			NewEntityCredentialsPanel panel = factory.getObject();
			panel.init(credReqId);
			return panel;
		}
	}

	List<SingleCredentialPanel.ObtainedCredential> getCredentials()
	{
		return panels.stream().map(SingleCredentialPanel::getCredential).filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}
	
	private void init(String credReqId)
	{
		this.credReqId = credReqId;
		add(new Hr());
		loadCredentials();
		if (credentials.isEmpty())
		{
			add(new Span(
					msg.getMessage("CredentialChangeDialog.noCredentials")));
			return;
		}
		panels = new ArrayList<>();	
		
		for (CredentialDefinition credDef : credentials.values())
		{
			SingleCredentialPanel panel = new SingleCredentialPanel(msg, credMan, credEditorReg, credDef);
			if (panel.isNotEmptyEditor())
				panels.add(panel);
		}
		
		int last = panels.size();
		int credSize = panels.size();
		for (SingleCredentialPanel panel: panels)
		{
			if (last > 0 && last < credSize)
				add(new Hr());
			add(panel);
			panel.setPadding(false);
			panel.setSpacing(false);
			last--;
		}
		
		add(new Hr());
		setPadding(false);
		setSpacing(false);
		setSizeFull();
	}

	
	private void loadCredentials()
	{
		CredentialRequirements credReq = null;
		Collection<CredentialDefinition> allCreds;
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
