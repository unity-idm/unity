/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.List;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilder;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.RegistrationLayoutsContainer;

/**
 * Generates a UI based on a given {@link EnquiryForm}. 
 * @author K. Benedyczak
 */
public class EnquiryResponseEditor extends BaseRequestEditor<EnquiryResponse>
{
	private final EnquiryForm enquiryForm;
	private final PrefilledSet prefilled;
	private final List<PolicyAgreementConfiguration> filteredPolicyAgreement;
	private RegistrationLayoutsContainer layoutContainer;
	
	public EnquiryResponseEditor(MessageSource msg, EnquiryForm form,
			RemotelyAuthenticatedPrincipal remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement atMan, CredentialManagement credMan,
			GroupsManagement groupsMan, ImageAccessService imageAccessService,
			PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder,
			List<PolicyAgreementConfiguration> filteredPolicyAgreement,
			PrefilledSet prefilled) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, atMan, credMan, groupsMan, imageAccessService, 
				policyAgreementsRepresentationBuilder);
		this.enquiryForm = form;
		this.filteredPolicyAgreement = filteredPolicyAgreement;
		this.prefilled = prefilled;

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
	
	
	
	private void initUI() throws EngineException
	{
		layoutContainer = createLayouts();
		
		createControls(layoutContainer, enquiryForm.getEffectiveFormLayout(msg), prefilled);
	}
	
	String getPageTitle()
	{
		return enquiryForm.getPageTitle() == null ? null : enquiryForm.getPageTitle().getValue(msg);
	}
	
	boolean isOptional()
	{
		return enquiryForm.getType() == EnquiryType.REQUESTED_OPTIONAL;
	}

	void focusFirst()
	{
		focusFirst(layoutContainer.registrationFormLayout);
	}

	@Override
	protected boolean isPolicyAgreementsIsFiltered(PolicyAgreementConfiguration toCheck)
	{
		return !filteredPolicyAgreement.contains(toCheck);
	}	
}


