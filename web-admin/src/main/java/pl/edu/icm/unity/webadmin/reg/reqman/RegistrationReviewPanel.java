/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;

/**
 * Shows request contents and provides a possibility to edit it.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RegistrationReviewPanel extends RequestReviewPanelBase
{
	private RegistrationRequestState requestState;
	private Label code;
	private GroupsManagement groupMan;
	
	@Autowired
	public RegistrationReviewPanel(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, IdentityFormatter idFormatter, GroupsManagement groupMan)
	{
		super(msg, handlersRegistry, idTypesRegistry, idFormatter);
		this.groupMan = groupMan;
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(new MarginInfo(true, false));
		
		code = new Label(msg.getMessage("RequestReviewPanel.codeValid"));
		code.addStyleName(Styles.bold.toString());
		
		main.addComponents(code);
		super.addStandardComponents(main, msg.getMessage("RequestReviewPanel.requestedGroups"));
		setCompositionRoot(main);
	}
	
	public RegistrationRequest getUpdatedRequest()
	{
		RegistrationRequest ret = new RegistrationRequest();
		fillRequest(ret);
		RegistrationRequest orig = requestState.getRequest();
		ret.setRegistrationCode(orig.getRegistrationCode());
		return ret;
	}
	
	public void setInput(RegistrationRequestState requestState, RegistrationForm form)
	{
		this.requestState = requestState;
		super.setInput(requestState, form);
		code.setVisible(requestState.getRequest().getRegistrationCode() != null);
		setGroupEntries(getGroupEntries(requestState, form));	
	}

	private List<Component> getGroupEntries(RegistrationRequestState requestState, RegistrationForm form)
	{
		List<Component> groupEntries = new ArrayList<>();
		RegistrationRequest request = requestState.getRequest();
		for (int i = 0; i < request.getGroupSelections().size(); i++)
		{
			GroupSelection selection = request.getGroupSelections().get(i);
			if (form.getGroupParams().size() <= i)
				break;
			String selGroups = selection.getSelectedGroups().stream().sorted()
					.map(g -> getGroupDisplayedName(groupMan, g)).collect(Collectors.toList())
					.toString();
			String groupEntry = selection.getExternalIdp() == null ? selGroups
					: "[from: " + selection.getExternalIdp() + "] " + selGroups;
			groupEntries.add(new Label(groupEntry));
		}
		return groupEntries;
	}
}
