/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.policy_agreements;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.vaadin.elements.CheckboxWithError;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;

import java.util.List;


public class PolicyAgreementRepresentation extends HorizontalLayout
{
	private final List<Long> documentsIdsToAccept;
	private final CheckboxWithError decisionCheckBox;
	private final PolicyAgreementPresentationType presentationType;
	private final boolean mandatory;

	public PolicyAgreementRepresentation(List<Long> documentsToAccept, String representation,
	                                     PolicyAgreementPresentationType presentationType, boolean mandatory)
	{
		this.documentsIdsToAccept = documentsToAccept;
		this.decisionCheckBox = new CheckboxWithError(representation);
		this.mandatory = mandatory;
		this.presentationType = presentationType;
		initUI();
	}

	private void initUI()
	{
		if (presentationType == PolicyAgreementPresentationType.INFORMATIVE_ONLY)
		{
			decisionCheckBox.setValue(true);
			decisionCheckBox.setVisible(false);

		} else if (presentationType == PolicyAgreementPresentationType.CHECKBOX_SELECTED)
		{
			decisionCheckBox.setValue(true);
		}
		
		setWidthFull();
		setMargin(false);
		setPadding(false);
		add(decisionCheckBox);
	}

	public PolicyAgreementDecision getDecision()
	{
		return new PolicyAgreementDecision(
				decisionCheckBox.getState() ? PolicyAgreementAcceptanceStatus.ACCEPTED
						: PolicyAgreementAcceptanceStatus.NOT_ACCEPTED,
				documentsIdsToAccept);
	}

	public boolean isValid()
	{
		return mandatory ? decisionCheckBox.getState() : true;
	}

	public void addValueChangeListener(Runnable valueChangeListener)
	{
		decisionCheckBox.addValueChangeListener(e -> valueChangeListener.run());
	}

	public void setErrorMessage(String message)
	{
		decisionCheckBox.setInvalid(message != null);
		decisionCheckBox.setErrorMessage(message);
	}
}
