package com.photosphere.dynamic.generator;

import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.photosphere.test.logging.SimpleLogging;

public class ZkDynamicGeneratorTest {
	static {
		SimpleLogging.forPackageDebug();
	}
	private static final Logger LOG = LoggerFactory.getLogger(ZkDynamicGeneratorTest.class);

	TestingServer zkServer;

	@Before
	public void setUp() throws Exception {
		zkServer = new TestingServer(7777);
		zkServer.start();
	}

	@After
	public void tearDown() throws Exception {
		try {
			zkServer.stop();
		} catch (Exception ignore) {
		}
	}

	@SuppressWarnings("resource")
	@Test
	public void test() {
		String connString = zkServer.getConnectString();
		String watchPath = ZKPaths.makePath("TEST", "WATCH");
		String dataPath = ZKPaths.makePath("TEST", "DATA");
		ZkRegister zkRegister = null;
		try {
			zkRegister = new ZkRegister(connString, watchPath, dataPath);
		} catch (Exception e) {
			LOG.error("could not open", e);
			fail("could not create");
			return;
		}
		ZkDynamicGenerator zkDynamicGenerator = new ZkDynamicGenerator(connString, watchPath, dataPath);
		DynamicGenerator generator = DynamicGenerator.instance;
		zkDynamicGenerator.start();
		LOG.debug("started.");
		String pkg = getClass().getPackage().getName();
		for (int i = 0; i < 10; i++) {
			String alias = getClass().getSimpleName() + "_Object" + i;
			String fqcn = fqcn(pkg, alias);
			String source = source(pkg, alias);
			try {
				zkRegister.register(alias, fqcn, source);
			} catch (Exception e) {
				LOG.error("could not register", e);
			}
		}
		try {
			for (int i = 0; i < 10; i++) {
				String cls = getClass().getSimpleName() + "_Object" + i;
				while (!generator.isLoadedAlias(cls)) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ignore) {
					}
				}
				Closeable closeable = generator.createInstanceByAlias(cls);
				closeable.close();
			}
			zkDynamicGenerator.close();
			zkRegister.close();
		} catch (Exception e) {
			LOG.error("could not create", e);
			fail("could not create");
		}
	}

	private String fqcn(String pkg, String cls) {
		return String.join(".", pkg, cls);
	}

	private String source(String pkg, String cls) {
		return String.join("\n" //
				, "package " + pkg + ";" //
				, "" //
				, "public class " + cls + " implements " + Closeable.class.getName() + " {" //
				, "  public void close() throws " + IOException.class.getName() + " {" //
				, "    System.out.println(\"test! " + cls + "\");" //
				, "  }" //
				, "}" //
		);
	}

}
