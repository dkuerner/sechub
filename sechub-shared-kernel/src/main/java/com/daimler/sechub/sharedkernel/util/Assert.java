// SPDX-License-Identifier: MIT
package com.daimler.sechub.sharedkernel.util;

import java.util.Collection;

/**
 * Some simple assert methods
 * 
 * @author Albert Tregnaghi
 *
 */
public class Assert {

	private Assert() {

	}

	/**
	 * Throws an illegal argument exception when given object is <code>null</code>
	 * 
	 * @param obj
	 * @param message
	 */
	public static void notNull(Object obj, String message) {
		if (obj == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Throws an illegal argument exception when given string is <code>null</code> or empty
	 * 
	 * @param string
	 * @param message
	 */
	public static void notEmpty(String string, String message) {
		if (string == null || string.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}
	
	/**
	 * Throws an illegal argument exception when given collection is <code>null</code> or empty
	 * 
	 * @param string
	 * @param message
	 */
	public static void notEmpty(Collection<?> collection, String message) {
		if (collection == null || collection.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}
}
