/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.requests;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilderV8;

/**
 * Shows request contents and provides a possibility to edit it.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
class RegistrationReviewPanelV8 extends RequestReviewPanelBase
{
	private RegistrationRequestState requestState;
	private Label code;
	
	@Autowired
	RegistrationReviewPanelV8(MessageSource msg, AttributeHandlerRegistryV8 handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, IdentityFormatter idFormatter, GroupsManagement groupMan,
			PolicyDocumentManagement policyDocMan,
			PolicyAgreementRepresentationBuilderV8 policyAgreementRepresentationBuilder)
	{
		super(msg, handlersRegistry, idTypesRegistry, idFormatter, groupMan, policyDocMan,
				policyAgreementRepresentationBuilder);

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
	
	RegistrationRequest getUpdatedRequest()
	{
		RegistrationRequest ret = new RegistrationRequest();
		fillRequest(ret);
		RegistrationRequest orig = requestState.getRequest();
		ret.setRegistrationCode(orig.getRegistrationCode());
		return ret;
	}
	
	void setInput(RegistrationRequestState requestState, RegistrationForm form)
	{
		this.requestState = requestState;
		super.setInput(requestState, form);
		code.setVisible(requestState.getRequest().getRegistrationCode() != null);
		setGroupEntries(getGroupEntries(requestState, form));	
	}

	private List<Component> getGroupEntries(RegistrationRequestState requestState, RegistrationForm form)
	{
		return super.getGroupEntries(requestState, form, Arrays.asList(), false);
	}
}
