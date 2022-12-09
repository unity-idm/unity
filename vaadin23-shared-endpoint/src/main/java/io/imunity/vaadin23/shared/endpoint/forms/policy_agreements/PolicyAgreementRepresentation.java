/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.forms.policy_agreements;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin23.shared.endpoint.components.CheckboxWithError;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;

import java.util.List;


public class PolicyAgreementRepresentation extends VerticalLayout
{
	private final List<Long> documentsIdsToAccept;
	private final CheckboxWithError decisionCheckBox;
	private final String representation;
	private final PolicyAgreementPresentationType presentationType;
	private final boolean mandatory;

	public PolicyAgreementRepresentation(List<Long> documentsToAccept, String representation,
	                                     PolicyAgreementPresentationType presentationType, boolean mandatory)
	{
		this.documentsIdsToAccept = documentsToAccept;
		this.decisionCheckBox = new CheckboxWithError();
		this.representation = representation;
		this.mandatory = mandatory;
		this.presentationType = presentationType;
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout main = new HorizontalLayout();
		main.setWidth(100, Unit.PERCENTAGE);
		main.setSpacing(false);

		Html caption = new Html(representation);

		if (presentationType == PolicyAgreementPresentationType.INFORMATIVE_ONLY)
		{
			decisionCheckBox.setValue(true);
			decisionCheckBox.setVisible(false);

		} else if (presentationType == PolicyAgreementPresentationType.CHECKBOX_SELECTED)
		{
			decisionCheckBox.setValue(true);
		}
		
		main.add(decisionCheckBox);
		main.add(caption);
		
		setWidth(100, Unit.PERCENTAGE);
		add(main);
	}

	public PolicyAgreementDecision getDecision()
	{
		return new PolicyAgreementDecision(
				decisionCheckBox.getValue() ? PolicyAgreementAcceptanceStatus.ACCEPTED
						: PolicyAgreementAcceptanceStatus.NOT_ACCEPTED,
				documentsIdsToAccept);
	}

	public boolean isValid()
	{
		return mandatory ? mandatory && decisionCheckBox.getValue() : true;
	}

	public void addValueChangeListener(Runnable valueChangeListener)
	{
		decisionCheckBox.addValueChangeListener(e -> valueChangeListener.run());
	}

	public void setErrorMessage(String message)
	{
		decisionCheckBox.setErrorMessage(message);
	}
}
