// SPDX-License-Identifier: MIT
package com.daimler.sechub.docgen.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import com.daimler.sechub.sharedkernel.usecases.UseCaseRestDoc;

/**
 * Factory to create parts belonging to rest doc.
 * 
 * @author Albert Tregnaghi
 *
 */
public class RestDocPathFactory {
	public static final String UC_RESTDOC = "uc_restdoc";

	private static Set<String> alreadyCreatedPathes = new HashSet<>();

	private RestDocPathFactory() {

	}

	/**
	 * Creates the name for the link to the rest documentation of the usecase
	 * 
	 * @param useCase
	 * @return name
	 */
	public static String createPath(Class<? extends Annotation> useCase) {
		return createPath(useCase, null);
	}

	/**
	 * Creates the name for the link to the rest documentation of the usecase
	 * 
	 * @param useCase
	 * @param variant
	 *            a variant or <code>null</code>. A variant is used when same
	 *            usecases got different variants -e.g. on reporting to differ
	 *            between "HTML" and "JSON" output variants...
	 * @return name
	 */
	public static String createPath(Class<? extends Annotation> useCase, String variant) {
		StringBuilder sb = new StringBuilder();

		sb.append(UC_RESTDOC);
		sb.append("/");
		sb.append(createIdentifier(useCase));
		sb.append("/");
		if (variant == null || variant.isEmpty()) {
			sb.append(UseCaseRestDoc.DEFAULT_VARIANT);
		} else {
			sb.append(variant);
		}
		String path = sb.toString();
		if (alreadyCreatedPathes.contains(path)) {
			throw new IllegalStateException("The path: " + path
					+ "\nis already created.\n\nThis means that a restdoc test did use this path already - and this is odd!\n\nPlease check if you have accidently copied a testcase and reused path cration for old usecase class!");
		}
		alreadyCreatedPathes.add(path);
		return path;
	}

	public static String createIdentifier(Class<? extends Annotation> useCase) {
		return useCase.getSimpleName().toLowerCase();
	}
}