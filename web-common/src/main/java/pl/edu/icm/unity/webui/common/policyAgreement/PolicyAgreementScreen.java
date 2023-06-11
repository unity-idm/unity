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

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
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
	private MessageSource msg;
	private PolicyAgreementManagement policyAgreementDecider;

	private Button submitButton;
	private List<PolicyAgreementRepresentation> agreements;
	private PolicyAgreementRepresentationBuilderV8 policyAgreementRepresentationBuilder;
	private Label titleLabel;
	private Label infoLabel;
	private VerticalLayout contents;
	private Runnable submitHandler;
	private VerticalLayout mainCenter;

	public PolicyAgreementScreen(MessageSource msg,
			PolicyAgreementRepresentationBuilderV8 policyAgreementRepresentationBuilder,
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
			contents.setComponentAlignment(agrRep, Alignment.MIDDLE_CENTER);
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

	public PolicyAgreementScreen withWidht(float width, String unit)
	{
		mainCenter.setWidth(width, Unit.getUnitFromSymbol(unit));
		return this;
	}
	
	public PolicyAgreementScreen withSubmitHandler(Runnable submitHandler)
	{
		this.submitHandler = submitHandler;
		return this;
	}

	public PolicyAgreementScreen init()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		mainCenter = new VerticalLayout();		
		mainCenter.setMargin(false);
		mainCenter.setSpacing(false);
		contents = new VerticalLayout();
		mainCenter.addComponent(contents);
		mainCenter.setComponentAlignment(contents, Alignment.MIDDLE_CENTER);
		titleLabel = new Label();
		titleLabel.setStyleName(Styles.viewTitle.toString());
		titleLabel.setVisible(false);
		infoLabel = new Label();
		infoLabel.setStyleName(Styles.viewSubtitle.toString());
		infoLabel.setVisible(false);
		Label space = new Label();
		contents.addComponent(titleLabel);
		contents.setComponentAlignment(titleLabel, Alignment.MIDDLE_CENTER);
		contents.addComponent(infoLabel);
		contents.setComponentAlignment(infoLabel, Alignment.MIDDLE_CENTER);
		contents.addComponent(space);
		contents.setComponentAlignment(space, Alignment.MIDDLE_CENTER);
		
		submitButton = new Button(msg.getMessage("submit"));
		submitButton.addStyleName(Styles.buttonAction.toString());
		submitButton.addClickListener(e -> submit());
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setHeight(100, Unit.PERCENTAGE);
		wrapper.addStyleName("u-policyAgreementColumn");
		wrapper.addComponent(submitButton);
		wrapper.setComponentAlignment(submitButton, Alignment.TOP_CENTER);
		mainCenter.addComponent(wrapper);
		mainCenter.setComponentAlignment(wrapper, Alignment.MIDDLE_CENTER);
		main.addComponent(mainCenter);
		main.setComponentAlignment(mainCenter, Alignment.MIDDLE_CENTER);
		refreshSubmitButton();
		setCompositionRoot(main);
		setSizeFull();
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
