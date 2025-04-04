/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.enquiry;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.forms.BaseRequestEditor;
import io.imunity.vaadin.endpoint.common.forms.PrefilledSet;
import io.imunity.vaadin.endpoint.common.forms.RegistrationLayoutsContainer;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

import java.util.List;
import java.util.Map;

public class EnquiryResponseEditor extends BaseRequestEditor<EnquiryResponse>
{
	private final EnquiryForm enquiryForm;
	private final PrefilledSet prefilled;
	private final List<PolicyAgreementConfiguration> filteredPolicyAgreement;
	private final Map<String, Object> messageParams;

	public EnquiryResponseEditor(MessageSource msg, EnquiryForm form,
	                             RemotelyAuthenticatedPrincipal remotelyAuthenticated,
	                             IdentityEditorRegistry identityEditorRegistry,
	                             CredentialEditorRegistry credentialEditorRegistry,
	                             AttributeHandlerRegistry attributeHandlerRegistry,
	                             AttributeTypeManagement atMan, CredentialManagement credMan,
	                             GroupsManagement groupsMan, NotificationPresenter notificationPresenter,
	                             PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder,
	                             List<PolicyAgreementConfiguration> filteredPolicyAgreement,
	                             PrefilledSet prefilled, VaadinLogoImageLoader logoImageLoader,
	                             Map<String, Object> messageParams) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, atMan, credMan, groupsMan, notificationPresenter,
				policyAgreementsRepresentationBuilder, logoImageLoader);
		this.enquiryForm = form;
		this.filteredPolicyAgreement = filteredPolicyAgreement;
		this.prefilled = prefilled;
		this.messageParams = messageParams;

		validateMandatoryRemoteInput();
		initUI();
	}
	
	@Override
	public EnquiryResponse getRequest(boolean withCredentials) throws FormValidationException
	{
		EnquiryResponse ret = new EnquiryResponse();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status, withCredentials);
		
		if (status.hasFormException)
			throw new FormValidationException();
		
		return ret;
	}
	
	
	
	private void initUI()
	{
		RegistrationLayoutsContainer layoutContainer = createLayouts(messageParams);
		
		createControls(layoutContainer, enquiryForm.getEffectiveFormLayout(msg), prefilled);
	}

	@Override
	protected boolean isPolicyAgreementsIsFiltered(PolicyAgreementConfiguration toCheck)
	{
		return !filteredPolicyAgreement.contains(toCheck);
	}	
}


