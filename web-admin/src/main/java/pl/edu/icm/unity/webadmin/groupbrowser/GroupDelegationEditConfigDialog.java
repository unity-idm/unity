/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.groupbrowser;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Edit dialog for {@link GroupDelegationConfiguration}. 
 * @author P.Piernik
 *
 */
public class GroupDelegationEditConfigDialog extends AbstractDialog
{
	private Consumer<Group> callback;
	private Group group;
	private TextField logoUrl;
	private CheckBox enableDelegation;
	private ComboBox<String> registratioFormCombo;
	private ComboBox<String> signupEnquiryFormCombo;
	private ComboBox<String> stickyEnquiryFormCombo;
	private Binder<GroupDelegationConfiguration> binder;

	private RegistrationsManagement registrationMan;
	private EnquiryManagement enquiryMan;

	public GroupDelegationEditConfigDialog(UnityMessageSource msg,
			RegistrationsManagement registrationMan, EnquiryManagement enquiryMan,
			Group group, Consumer<Group> callback)
	{
		super(msg, msg.getMessage("GroupDelegationEditConfigDialog.caption"),
				msg.getMessage("ok"), msg.getMessage("cancel"));
		this.group = group;
		this.callback = callback;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
	}

	@Override
	protected Component getContents() throws Exception
	{

		enableDelegation = new CheckBox(msg.getMessage(
				"GroupDelegationEditConfigDialog.enableDelegationCaption"));
		logoUrl = new TextField(
				msg.getMessage("GroupDelegationEditConfigDialog.logoUrlCaption"));

		registratioFormCombo = new ComboBox<String>(
				msg.getMessage("GroupDelegationEditConfigDialog.registrationForm"));

		//TODO fill all comboBoxes with good values
		List<RegistrationForm> forms = registrationMan.getForms();
		registratioFormCombo.setItems(
				forms.stream().map(f -> f.getName()).collect(Collectors.toList()));

		signupEnquiryFormCombo = new ComboBox<String>(
				msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiry"));
		List<EnquiryForm> enquires = enquiryMan.getEnquires();
		signupEnquiryFormCombo.setItems(enquires.stream().map(f -> f.getName())
				.collect(Collectors.toList()));

		stickyEnquiryFormCombo = new ComboBox<String>(
				msg.getMessage("GroupDelegationEditConfigDialog.stickyEnquiry"));
		stickyEnquiryFormCombo.setItems(enquires.stream().map(f -> f.getName())
				.collect(Collectors.toList()));

		binder = new Binder<>(GroupDelegationConfiguration.class);
		binder.forField(enableDelegation).bind("enabled");
		binder.forField(logoUrl).bind("logoUrl");
		binder.forField(registratioFormCombo).bind("registratioForm");
		binder.forField(stickyEnquiryFormCombo).bind("stickyEnquiryForm");
		binder.forField(signupEnquiryFormCombo).bind("signupEnquiryForm");
		binder.setBean(group.getDelegationConfiguration());

		FormLayout main = new FormLayout();
		main.addComponents(enableDelegation, logoUrl, registratioFormCombo,
				signupEnquiryFormCombo, stickyEnquiryFormCombo);
		return main;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			group.setDelegationConfiguration(binder.getBean());
			callback.accept(group);
			close();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"GroupDelegationEditConfigDialog.cannotUpdate"), e);
		}
	}
}
