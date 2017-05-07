/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.manifest.yaml;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget.CFTarget;
import org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget.CFTargetCache;
import org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget.ConnectionException;
import org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget.NoTargetsException;
import org.springframework.ide.vscode.commons.util.Assert;
import org.springframework.ide.vscode.commons.util.ExceptionUtil;
import org.springframework.ide.vscode.commons.util.ValueParseException;
import org.springframework.ide.vscode.commons.yaml.schema.YValueHint;

import com.google.common.collect.ImmutableList;

public abstract class AbstractCFHintsProvider implements Callable<Collection<YValueHint>> {

	public static final String EMPTY_VALUE = "";
	protected final CFTargetCache targetCache;

	public AbstractCFHintsProvider(CFTargetCache targetCache) {
		Assert.isNotNull(targetCache);
		this.targetCache = targetCache;
	}

	/**
	 * Used in error messages. For example "Failed to get ${type-name}s from Cloudfoundry".
	 * @return
	 */
	protected abstract String getTypeName();

	@Override
	public Collection<YValueHint> call() throws Exception {

		try {
			List<CFTarget> targets = targetCache.getOrCreate();

			// Do NOT wrap the results in another list. Allow null values to return
			// as the reconcile framework expects null if hints failed to be resolved
			return getHints(targets);
		} catch (Throwable e) {
			// Convert any error into something readable to the user as it may
			// appear in the content assist
			// UI. Do NOT wrap the original exception as the framework may look
			// for the deepest cause when
			// resolving the error message. Instead, log the full error, and
			// only throw a
			// new exception with a "nicer" message
			Throwable errorNoAppending = getErrorNoAppending(e);
			if (errorNoAppending != null) {
				// Do not log the no-targets exception as it may be encountered
				// frequently
				// if a user does not have a CF client installed
				throw new ValueParseException(ExceptionUtil.getMessageNoAppendedInformation(errorNoAppending));
			} else {
				// Log any other error
				//logger.log(Level.SEVERE, ExceptionUtil.getMessage(e), e);
				throw new ValueParseException(
						"Failed to get "+getTypeName()+"s from Cloud Foundry: "+ExceptionUtil.getMessage(e));
			}
		}
	}

	/**
	 *
	 * @param e
	 * @return an error that requires no additional information when showing its
	 *         message, or null if no such error is found
	 */
	protected Throwable getErrorNoAppending(Throwable e) {
		return ExceptionUtil.findThrowable(e,
				ImmutableList.of(NoTargetsException.class, ConnectionException.class));
	}

	/**
	 *
	 * @return non-null list of hints. Return empty if no hints available
	 */
	abstract protected Collection<YValueHint> getHints(List<CFTarget> targets) throws Exception;

}
