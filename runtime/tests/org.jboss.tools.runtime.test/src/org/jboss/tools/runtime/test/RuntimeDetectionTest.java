/*************************************************************************************
 * Copyright (c) 2011 JBoss by Red Hat and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.runtime.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.tools.runtime.core.JBossRuntimeLocator;
import org.jboss.tools.runtime.core.RuntimeCoreActivator;
import org.jboss.tools.runtime.core.model.IRuntimeDetector;
import org.jboss.tools.runtime.core.model.RuntimePath;
import org.jboss.tools.runtime.core.model.ServerDefinition;
import org.jboss.tools.runtime.ui.RuntimeUIActivator;
import org.jboss.tools.seam.core.project.facet.SeamRuntime;
import org.jboss.tools.seam.core.project.facet.SeamRuntimeManager;
import org.jboss.tools.seam.core.project.facet.SeamVersion;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class RuntimeDetectionTest {
	private final static String seamVersionAttributeName = "Seam-Version";

	@BeforeClass
	public static void create() {
		RuntimeCoreActivator.getDefault();
		RuntimeUIActivator.getDefault();
		addRuntimePaths();
		createRuntimes();
	}

	private static void addRuntimePaths() {
		List<RuntimePath> runtimePaths = RuntimeUIActivator.getDefault()
				.getRuntimePaths();
		String[] paths = { IRuntimeDetectionConstants.JBOSS_42_HOME,
				IRuntimeDetectionConstants.JBOSS_51_HOME,
				IRuntimeDetectionConstants.SEAM_20_HOME,
				IRuntimeDetectionConstants.SEAM_22_HOME,
				IRuntimeDetectionConstants.EAP_43_HOME };

		for (String path : paths) {
			RuntimePath runtimePath = new RuntimePath(path);
			runtimePath.setScanOnEveryStartup(false);
			runtimePaths.add(runtimePath);
		}
		RuntimeUIActivator.getDefault().saveRuntimePaths();
		List<ServerDefinition> serverDefinitions = new ArrayList<ServerDefinition>();
		Set<IRuntimeDetector> detectors = RuntimeCoreActivator
				.getRuntimeDetectors();
		for (IRuntimeDetector detector : detectors) {
			if (detector.isEnabled()) {
				detector.initializeRuntimes(serverDefinitions);
			}
		}
	}

	private static void createRuntimes() {
		JBossRuntimeLocator locator = new JBossRuntimeLocator();
		List<RuntimePath> runtimePaths = RuntimeUIActivator.getDefault()
				.getRuntimePaths();
		for (RuntimePath runtimePath : runtimePaths) {
			List<ServerDefinition> serverDefinitions = locator
					.searchForRuntimes(runtimePath.getPath(),
							new NullProgressMonitor());
			runtimePath.getServerDefinitions().clear();
			for (ServerDefinition serverDefinition : serverDefinitions) {
				serverDefinition.setRuntimePath(runtimePath);
			}
			runtimePath.getServerDefinitions().addAll(serverDefinitions);
		}
		List<ServerDefinition> serverDefinitions = RuntimeUIActivator
				.getDefault().getServerDefinitions();
		Set<IRuntimeDetector> detectors = RuntimeCoreActivator
				.getRuntimeDetectors();
		for (IRuntimeDetector detector : detectors) {
			if (detector.isEnabled()) {
				detector.initializeRuntimes(serverDefinitions);
			}
		}
	}

	@Test
	public void testRuntimeDetectors() {
		Set<IRuntimeDetector> detectors = RuntimeCoreActivator
				.getRuntimeDetectors();
		assertTrue("Runtime detectors don't exist.", detectors.size() > 0);
	}

	@Test
	public void testRuntimePaths() {
		List<RuntimePath> runtimePaths = RuntimeUIActivator.getDefault()
				.getRuntimePaths();
		assertTrue(
				"runtimePaths.size()\nExpected: 5\nWas: " + runtimePaths.size(),
				runtimePaths.size() == 5);
	}
	
	@Test
	public void testRuntimePathsExists() {
		List<RuntimePath> runtimePaths = RuntimeUIActivator.getDefault()
				.getRuntimePaths();
		for (RuntimePath runtimePath:runtimePaths) {
			String path = runtimePath.getPath();
			File file = new File(path);
			assertTrue("The '" + file.getAbsolutePath()
					+ "' path isn't valid.", file.isDirectory());
		}
	}

	@Test
	public void testLocations() {
		List<ServerDefinition> serverDefinitions = RuntimeUIActivator
				.getDefault().getServerDefinitions();
		for (ServerDefinition serverDefinition : serverDefinitions) {
			File location = serverDefinition.getLocation();
			assertTrue("The '" + location.getAbsolutePath()
					+ "' path isn't valid.", location.isDirectory());
		}
	}
	
	@Test
	public void testSeam22Location() throws Exception {
		String seamHome = IRuntimeDetectionConstants.SEAM_22_HOME;
		testSeamHome(seamHome, "2.2");
	}
	
	@Test
	public void testSeam20Location() throws Exception {
		String seamHome = IRuntimeDetectionConstants.SEAM_20_HOME;
		testSeamHome(seamHome, "2.0");
	}

	private void testSeamHome(String seamHome, String seamVersion) throws IOException {
		File file = new File(seamHome);
		assertTrue("The '" + file.getAbsolutePath()
				+ "' path isn't valid.", file.isDirectory());
		String[] seamFiles = file.list(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				if ("seam-gen".equals(name)) {
					return true;
				}
				if ("lib".equals(name)) {
					return true;
				}
				return false;
			}
		});
		assertTrue("seamFiles : " + seamFiles, seamFiles != null
				&& seamFiles.length == 2);
		File jarFile = new File(seamHome, "lib/jboss-seam.jar");
		assertTrue("The '" + jarFile.getAbsolutePath() + "' path isn't valid.",
				jarFile.isFile());
		JarFile jar = new JarFile(jarFile);
		Attributes attributes = jar.getManifest().getMainAttributes();
		String version = attributes.getValue(seamVersionAttributeName);
		assertTrue("seamVersion: " + version, version != null && version.startsWith(seamVersion));
	}
	
	@Test
	public void testServerDefinitions() {
		List<ServerDefinition> serverDefinitions = RuntimeUIActivator
				.getDefault().getServerDefinitions();
		List<RuntimePath> runtimePaths = RuntimeUIActivator.getDefault()
			.getRuntimePaths();
		assertTrue("serverDefinitions.size()\nExpected: 5\nWas: "
				+ serverDefinitions.size() + 
				"\nserverDefinitions: " + serverDefinitions +
				"\nruntimePaths=" + runtimePaths, serverDefinitions.size() == 5);
	}

	@Test
	public void testSeam22() {
		SeamRuntime[] seamRuntimes = SeamRuntimeManager.getInstance()
				.getRuntimes();
		int count = 0;
		for (SeamRuntime seamRuntime : seamRuntimes) {
			SeamVersion version = seamRuntime.getVersion();
			if (SeamVersion.SEAM_2_2.equals(version)) {
				count++;
			}
		}
		assertTrue("Seam 2.2\nExpected: 1\nWas: " + count, count == 1);
	}

	@Test
	public void testSeam20() {
		SeamRuntime[] seamRuntimes = SeamRuntimeManager.getInstance()
				.getRuntimes();
		int count = 0;
		for (SeamRuntime seamRuntime : seamRuntimes) {
			SeamVersion version = seamRuntime.getVersion();
			if (SeamVersion.SEAM_2_0.equals(version)) {
				count++;
			}
		}
		assertTrue("Seam 2.0\nExpected: 2\nWas: " + count, count == 2);
	}

	@Test
	public void testSeam12() {
		SeamRuntime[] seamRuntimes = SeamRuntimeManager.getInstance()
				.getRuntimes();
		int count = 0;
		for (SeamRuntime seamRuntime : seamRuntimes) {
			SeamVersion version = seamRuntime.getVersion();
			if (SeamVersion.SEAM_1_2.equals(version)) {
				count++;
			}
		}
		assertTrue("Seam 1.2\nExpected: 1\nWas: " + count, count == 1);
	}

	@Test
	public void testSeamRuntimes() {
		SeamRuntime[] seamRuntimes = SeamRuntimeManager.getInstance()
				.getRuntimes();
		assertTrue("seamRuntimes.length\nExpected: 4\nWas: "
				+ seamRuntimes.length, seamRuntimes.length == 4);
	}

	@Test
	public void testJBossAs42() {
		IRuntime[] runtimes = ServerCore.getRuntimes();
		int count = 0;
		for (IRuntime runtime : runtimes) {
			IRuntimeType runtimeType = runtime.getRuntimeType();
			if (IJBossToolingConstants.AS_42.equals(runtimeType.getId())) {
				count++;
			}
		}
		assertTrue("JBoss AS 4.2\nExpected: 1\nWas: " + count, count == 1);
	}

	@Test
	public void testJBossAs51() {
		IRuntime[] runtimes = ServerCore.getRuntimes();
		int count = 0;
		for (IRuntime runtime : runtimes) {
			IRuntimeType runtimeType = runtime.getRuntimeType();
			if (IJBossToolingConstants.AS_51.equals(runtimeType.getId())) {
				count++;
			}
		}
		assertTrue("JBoss AS 5.1\nExpected: 1\nWas: " + count, count == 1);
	}

	@Test
	public void testJBossEap43() {
		IRuntime[] runtimes = ServerCore.getRuntimes();
		int count = 0;
		for (IRuntime runtime : runtimes) {
			IRuntimeType runtimeType = runtime.getRuntimeType();
			if (IJBossToolingConstants.EAP_43.equals(runtimeType.getId())) {
				count++;
			}
		}
		assertTrue("JBoss EAP 4.3\nExpected: 1\nWas: " + count, count == 1);
	}

	@Test
	public void testWtpRuntimes() {
		IRuntime[] runtimes = ServerCore.getRuntimes();
		assertTrue("runtimes.length\nExpected: 3\nWas: " + runtimes.length,
				runtimes.length == 3);
	}
}