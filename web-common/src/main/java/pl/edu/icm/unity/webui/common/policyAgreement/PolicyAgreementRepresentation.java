/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.policyAgreement;

import java.util.List;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Represents single policy agreement. Shows single checkbox if needed and policy agreement text.
 * 
 * @author P.Piernik
 *
 */
public class PolicyAgreementRepresentation extends CustomComponent
{
	private final List<Long> documentsIdsToAccept;
	private final CheckBox decisionCheckBox;
	private final String representation;
	private final PolicyAgreementPresentationType presentationType;
	private final boolean mandatory;

	public PolicyAgreementRepresentation(List<Long> documentsToAccept, String representation,
			PolicyAgreementPresentationType presentationType, boolean mandatory)
	{
		this.documentsIdsToAccept = documentsToAccept;
		this.decisionCheckBox = new CheckBox();
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

		Label caption = new Label();
		caption.setWidth(100, Unit.PERCENTAGE);
		caption.setValue(representation);
		caption.addStyleName(Styles.policyAgreementItem.toString());
		caption.setContentMode(ContentMode.HTML);
		setCaptionAsHtml(true);

		if (presentationType == PolicyAgreementPresentationType.INFORMATIVE_ONLY)
		{
			decisionCheckBox.setValue(true);
			decisionCheckBox.setVisible(false);

		} else if (presentationType == PolicyAgreementPresentationType.CHECKBOX_SELECTED)
		{
			decisionCheckBox.setValue(true);
		}
		
		main.addComponent(decisionCheckBox);
		main.addComponent(caption);
		
		main.setExpandRatio(decisionCheckBox, 0);
		main.setExpandRatio(caption, 1);
		
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
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
}
