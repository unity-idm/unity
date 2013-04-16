/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSplitPanel;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

/**
 * Places the whole contents in container with margins.
 * The content is divided with the {@link VerticalSplitPanel} or {@link HorizontalSplitPanel}.
 * The two content components are placed in two split panels, with only one margin set, the one next to split bar.
 * The size is set to full.
 * @author K. Benedyczak
 */
public class CompositeSplitPanel extends VerticalLayout
{
	public CompositeSplitPanel(boolean vertical, boolean needsMargin, 
			Component first, Component second, int firstSpacePercent)
	{
		AbstractSplitPanel split;
		VerticalLayout vl1 = new VerticalLayout(first);
		vl1.setSizeFull();
		VerticalLayout vl2 = new VerticalLayout(second);
		vl2.setSizeFull();
		
		if (vertical)
		{
			split = new VerticalSplitPanel();
			vl1.setMargin(new MarginInfo(false, false, true, false));
			vl2.setMargin(new MarginInfo(true, false, false, false));
		} else
		{
			split = new HorizontalSplitPanel();
			vl1.setMargin(new MarginInfo(false, true, false, false));
			vl2.setMargin(new MarginInfo(false, false, false, true));
		}
		split.setFirstComponent(vl1);
		split.setSecondComponent(vl2);
		split.setSizeFull();
		split.setSplitPosition(firstSpacePercent, Unit.PERCENTAGE);
		addComponent(split);
		setMargin(needsMargin);
		setSizeFull();
	}
}
