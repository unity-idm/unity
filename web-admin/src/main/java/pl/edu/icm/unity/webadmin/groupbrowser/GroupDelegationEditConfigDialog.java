/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.groupbrowser;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

/**
 * Edit dialog for {@link GroupDelegationConfiguration}.
 * 
 * @author P.Piernik
 *
 */
public class GroupDelegationEditConfigDialog extends AbstractDialog
{
	private Consumer<GroupDelegationConfiguration> callback;
	private GroupDelegationConfiguration toEdit;
	private TextField logoUrl;
	private CheckBox enableDelegation;
	private ComboBox<String> registratioFormCombo;
	private ComboBox<String> signupEnquiryFormCombo;
	private ComboBox<String> stickyEnquiryFormCombo;
	private ChipsWithDropdown<String> attributes;
	private Binder<GroupDelegationConfiguration> binder;

	private RegistrationsManagement registrationMan;
	private EnquiryManagement enquiryMan;
	private AttributeTypeManagement attrTypeMan;

	public GroupDelegationEditConfigDialog(UnityMessageSource msg,
			RegistrationsManagement registrationMan, EnquiryManagement enquiryMan,
			AttributeTypeManagement attrTypeMan, GroupDelegationConfiguration toEdit,
			Consumer<GroupDelegationConfiguration> callback)
	{
		super(msg, msg.getMessage("GroupDelegationEditConfigDialog.caption"),
				msg.getMessage("ok"), msg.getMessage("cancel"));
		this.toEdit = toEdit;
		this.callback = callback;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.attrTypeMan = attrTypeMan;
	}

	private void enableEdit(boolean enabled)
	{
		logoUrl.setEnabled(enabled);
		registratioFormCombo.setEnabled(enabled);
		signupEnquiryFormCombo.setEnabled(enabled);
		stickyEnquiryFormCombo.setEnabled(enabled);
		attributes.setEnabled(enabled);
	}

	@Override
	protected Component getContents() throws Exception
	{

		enableDelegation = new CheckBox(msg.getMessage(
				"GroupDelegationEditConfigDialog.enableDelegationCaption"));
		enableDelegation.addValueChangeListener(e -> {
			enableEdit(e.getValue());
		});
		logoUrl = new TextField(
				msg.getMessage("GroupDelegationEditConfigDialog.logoUrlCaption"));

		registratioFormCombo = new ComboBox<String>(
				msg.getMessage("GroupDelegationEditConfigDialog.registrationForm"));

		// TODO fill all comboBoxes with good values
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

		attributes = new ChipsWithDropdown<>();
		attributes.setCaption(msg.getMessage("GroupDelegationEditConfigDialog.attributes"));
		attributes.setMaxSelection(4);

		Collection<AttributeType> attributeTypes = attrTypeMan.getAttributeTypes();
		attributes.setItems(attributeTypes.stream().map(a -> a.getName())
				.collect(Collectors.toList()));
		if (toEdit.getAttributes() != null)
		{
			attributes.setSelectedItems(toEdit.getAttributes().stream()
					.collect(Collectors.toList()));
		}

		binder = new Binder<>(GroupDelegationConfiguration.class);
		binder.forField(enableDelegation).bind("enabled");
		binder.forField(logoUrl).bind("logoUrl");
		binder.forField(registratioFormCombo).bind("registratioForm");
		binder.forField(stickyEnquiryFormCombo).bind("stickyEnquiryForm");
		binder.forField(signupEnquiryFormCombo).bind("signupEnquiryForm");
		binder.setBean(toEdit);
		enableEdit(toEdit.isEnabled());

		FormLayout main = new FormLayout();
		main.addComponents(enableDelegation, logoUrl, registratioFormCombo,
				signupEnquiryFormCombo, stickyEnquiryFormCombo, attributes);
		return main;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			GroupDelegationConfiguration groupDelConfig = binder.getBean();
			groupDelConfig.setAttributes(attributes.getSelectedItems());
			callback.accept(groupDelConfig);
			close();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"GroupDelegationEditConfigDialog.cannotUpdate"), e);
		}
	}
}
