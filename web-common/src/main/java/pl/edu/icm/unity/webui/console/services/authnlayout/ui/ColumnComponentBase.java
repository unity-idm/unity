/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui;

import java.util.function.Consumer;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.DragSourceExtension;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Base layout for all {@link ColumnComponent}s
 * 
 * @author P.Piernik
 *
 */
public abstract class ColumnComponentBase extends CustomComponent implements ColumnComponent
{
	protected MessageSource msg;
	private String title;
	private Images icon;
	private Consumer<ColumnComponent> removeElementListener;
	private Runnable dragStart;
	private Runnable dragStop;
	private VerticalLayout contentLayout;
	private Button removeButton;
	private Button modeButton;
	private HorizontalLayout header;

	public ColumnComponentBase(MessageSource msg, String title, Images icon, Runnable dragStart,
			Runnable dragStop, Consumer<ColumnComponent> removeElementListener)
	{
		this.msg = msg;
		this.title = title;
		this.icon = icon;
		this.dragStart = dragStart;
		this.dragStop = dragStop;
		this.removeElementListener = removeElementListener;
		initUI();
	}

	public void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setWidth(100, Unit.PERCENTAGE);
		main.setSpacing(false);
		main.setMargin(false);

		header = new HorizontalLayout();
		header.setMargin(new MarginInfo(false, true, false, true));
		header.setSpacing(false);
		header.setWidth(100, Unit.PERCENTAGE);

		HorizontalLayout leftButtons = new HorizontalLayout();
		leftButtons.setMargin(false);
		leftButtons.setSpacing(false);

		modeButton = new Button();
		modeButton.addStyleName(Styles.vButtonLink.toString());
		modeButton.addClickListener(e -> changeMode());
		modeButton.setVisible(false);

		leftButtons.addComponent(modeButton);
		leftButtons.setComponentAlignment(modeButton, Alignment.MIDDLE_LEFT);

		Label ic = new Label(icon.getHtml());
		ic.setContentMode(ContentMode.HTML);
		ic.addStyleName(Styles.mediumIcon.toString());
		leftButtons.addComponent(ic);
		leftButtons.setComponentAlignment(ic, Alignment.BOTTOM_LEFT);

		header.addComponent(leftButtons);
		header.setComponentAlignment(leftButtons, Alignment.MIDDLE_LEFT);

		Label captionL = new Label();
		captionL.addStyleName(Styles.bold.toString());
		captionL.setValue(title);
		header.addComponent(captionL);
		header.setComponentAlignment(captionL, Alignment.MIDDLE_CENTER);

		HorizontalLayout rightButtons = new HorizontalLayout();
		rightButtons.setMargin(false);

		Button remove = new Button();
		remove.setDescription(msg.getMessage("ColumnElementBase.remove"));
		remove.addStyleName(Styles.vButtonLink.toString());
		remove.setIcon(Images.close_small.getResource());
		remove.addClickListener(e -> {
			if (removeElementListener != null)
				removeElementListener.accept(this);
		});
		remove.setVisible(removeElementListener != null);
		rightButtons.addComponent(remove);

		header.addComponent(rightButtons);
		header.setComponentAlignment(rightButtons, Alignment.MIDDLE_RIGHT);
		header.addStyleName("u-columnHeader");
		main.addComponent(header);

		contentLayout = new VerticalLayout();
		contentLayout.setMargin(true);
		contentLayout.setSpacing(false);
		contentLayout.setVisible(false);
		main.addComponent(contentLayout);

		setStyleName("u-border");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);

		DragSourceExtension<ColumnComponentBase> dragSource = new DragSourceExtension<>(this);
		dragSource.setEffectAllowed(EffectAllowed.MOVE);
		dragSource.setDragData(this);

		dragSource.addDragStartListener(e -> dragStart.run());
		dragSource.addDragEndListener(e -> dragStop.run());
	}

	private void changeMode()
	{
		if (contentLayout.isVisible())
		{
			collapse();
		} else
		{
			expand();
		}
	}

	public void collapse()
	{
		contentLayout.setVisible(false);
		modeButton.setIcon(Images.caret_right.getResource());
	}

	public void expand()
	{
		contentLayout.setVisible(true);
		modeButton.setIcon(Images.caret_down.getResource());
	}

	protected void addContent(Component content)
	{
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.setSpacing(false);
		wrapper.setWidth(100, Unit.PERCENTAGE);
		wrapper.addComponent(content);
		wrapper.setComponentAlignment(content, Alignment.MIDDLE_CENTER);
		contentLayout.addComponent(wrapper);
		modeButton.setVisible(true);
		header.setMargin(new MarginInfo(false, true, false, false));
		expand();
	}

	public void setRemoveListener(Consumer<ColumnComponent> removeListener)
	{
		this.removeElementListener = removeListener;
		removeButton.setVisible(removeElementListener != null);
	}

	@Override
	public void refresh()
	{

	}

	@Override
	public void validate() throws FormValidationException
	{

	}

	@Override
	public void addValueChangeListener(Runnable valueChange)
	{

	}
}
