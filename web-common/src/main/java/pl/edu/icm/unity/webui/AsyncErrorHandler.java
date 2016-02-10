/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webui;

/**
 * Used to provide error handler which is used in async situations (i.e. when throws/catch pattern can't be used).
 * @author Krzysztof Benedyczak
 */
public interface AsyncErrorHandler
{
	void onError(Exception e);
}
