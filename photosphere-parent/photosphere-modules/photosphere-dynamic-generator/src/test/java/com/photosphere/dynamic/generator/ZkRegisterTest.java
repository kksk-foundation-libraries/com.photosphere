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

public class ZkRegisterTest {
	static {
		SimpleLogging.forPackageDebug();
	}
	private static final Logger LOG = LoggerFactory.getLogger(ZkRegisterTest.class);

	TestingServer zkServer;

	@Before
	public void setUp() throws Exception {
		zkServer = new TestingServer(7777);
		zkServer.start();
	}

	@After
	public void tearDown() throws Exception {
		zkServer.stop();
	}

	@Test
	public void test() {
		String connString = zkServer.getConnectString();
		String watchPath = ZKPaths.makePath("TEST", "WATCH");
		String dataPath = ZKPaths.makePath("TEST", "DATA");
		try (ZkRegister zkRegister = new ZkRegister(connString, watchPath, dataPath);) {
			String pkg = ZkRegisterTest.class.getPackage().getName();
			for (int i = 0; i < 10; i++) {
				String alias = ZkRegisterTest.class.getSimpleName() + "_Object" + i;
				String fqcn = fqcn(pkg, alias);
				String source = source(pkg, alias);
				try {
					zkRegister.register(alias, fqcn, source);
				} catch (Exception e) {
					LOG.error("could not register", e);
				}
			}
		} catch (Exception e) {
			LOG.error("could not open", e);
			fail("Not yet implemented");
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
				, "    // System.out.println(\"test! " + cls + "\");" //
				, "  }" //
				, "}" //
		);
	}

}
