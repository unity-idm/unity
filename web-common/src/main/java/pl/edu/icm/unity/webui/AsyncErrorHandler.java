/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

/**
 * Used to provide error handler which is used in async situations (i.e. when throws/catch pattern can't be used).
 * @author Krzysztof Benedyczak
 */
public interface AsyncErrorHandler
{
	void onError(Exception e);
}
