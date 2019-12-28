/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.webelements.tooltip;

import org.vaadin.risto.stepper.IntStepper;

public class IntStepperWithTooltip extends CustomFieldWithTooltip<Integer, IntStepper>
{
	public IntStepperWithTooltip(String caption, String tooltipInfo)
	{
		super(IntStepper::new, caption, tooltipInfo);
	}

	public void setMinValue(int minValue)
	{
		getField().setMinValue(minValue);
	}

	public void setMaxValue(int maxValue)
	{
		getField().setMaxValue(maxValue);
	}
}
