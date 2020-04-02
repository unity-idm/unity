/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.policyAgreement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Shows policy list of policy agreement representation with submit button.
 * Submit button is active only when all mandatory agreements is confirmed.
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class PolicyAgreementScreen extends CustomComponent
{
	private UnityMessageSource msg;
	private PolicyAgreementManagement policyAgreementDecider;

	private Button submitButton;
	private List<PolicyAgreementRepresentation> agreements;
	private PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder;
	private Label titleLabel;
	private Label infoLabel;
	private VerticalLayout contents;
	private Runnable submitHandler;

	public PolicyAgreementScreen(UnityMessageSource msg,
			PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder,
			PolicyAgreementManagement policyAgreementDecider)
	{
		this.msg = msg;
		this.policyAgreementRepresentationBuilder = policyAgreementRepresentationBuilder;
		this.policyAgreementDecider = policyAgreementDecider;
		agreements = new ArrayList<>();
		init();
	}

	public PolicyAgreementScreen withAgreements(List<PolicyAgreementConfiguration> agreementsConfigs)
	{
		for (PolicyAgreementConfiguration config : agreementsConfigs)
		{
			PolicyAgreementRepresentation agrRep = policyAgreementRepresentationBuilder
					.getAgreementRepresentation(config);
			agrRep.addValueChangeListener(() -> refreshSubmitButton());
			agreements.add(agrRep);
			contents.addComponent(agrRep);
			contents.setExpandRatio(agrRep, 1);
		}
		refreshSubmitButton();

		return this;
	}

	public PolicyAgreementScreen withTitle(I18nString title)
	{
		titleLabel.setValue(title.getValue(msg));
		titleLabel.setVisible(true);
		return this;
	}

	public PolicyAgreementScreen withInfo(I18nString info)
	{
		infoLabel.setValue(info.getValue(msg));
		infoLabel.setVisible(true);
		return this;
	}

	public PolicyAgreementScreen withSubmitHandler(Runnable submitHandler)
	{
		this.submitHandler = submitHandler;
		return this;
	}

	public PolicyAgreementScreen init()
	{
		VerticalLayout vmain = new VerticalLayout();
		vmain.setWidth(100, Unit.PERCENTAGE);
		vmain.setMargin(false);
		vmain.setSpacing(false);

		contents = new VerticalLayout();
		contents.setWidth(50, Unit.PERCENTAGE);
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.MIDDLE_CENTER);

		titleLabel = new Label();
		titleLabel.setStyleName(Styles.textTitle.toString());
		titleLabel.setVisible(false);
		infoLabel = new Label();
		infoLabel.setStyleName(Styles.textXLarge.toString());
		infoLabel.setVisible(false);
		contents.addComponent(titleLabel);
		contents.setComponentAlignment(titleLabel, Alignment.MIDDLE_CENTER);
		contents.addComponent(infoLabel);
		contents.setComponentAlignment(infoLabel, Alignment.MIDDLE_CENTER);

		submitButton = new Button(msg.getMessage("submit"));
		submitButton.addClickListener(e -> submit());

		vmain.addComponent(submitButton);
		vmain.setComponentAlignment(submitButton, Alignment.MIDDLE_CENTER);

		refreshSubmitButton();
		setCompositionRoot(vmain);
		setWidth(100, Unit.PERCENTAGE);
		return this;
	}

	private void submit()
	{
		try
		{
			policyAgreementDecider.submitDecisions(
					new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
					agreements.stream().map(a -> a.getDecision()).collect(Collectors.toList()));
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("PolicyAgreementScreen.submitError"), e);
			return;
		}

		if (submitHandler != null)
		{
			submitHandler.run();
		}
	}

	private void refreshSubmitButton()
	{
		submitButton.setEnabled(!agreements.stream().filter(c -> !c.isValid()).findFirst().isPresent());
	}
}
