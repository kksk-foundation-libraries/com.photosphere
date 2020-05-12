package com.photosphere.notification;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.photosphere.test.logging.SimpleLogging;

public class NotificationTest {
	static {
		SimpleLogging.forPackageDebug();
	}
	private static final Logger LOG = LoggerFactory.getLogger(NotificationTest.class);

	private TestingServer zkServer;

	@Before
	public void setUp() throws Exception {
		InstanceSpec spec = new InstanceSpec(null, -1, -1, -1, true, -1, -1, -1, null, "0.0.0.0");
		zkServer = new TestingServer(spec, false);
		zkServer.start();
	}

	@After
	public void tearDown() throws Exception {
		try {
			zkServer.stop();
			zkServer.close();
		} catch (IOException e) {
		}
	}

	@Test
	public void test() {
		String connectString = zkServer.getConnectString();
		LOG.debug(connectString);
		String watchPath = String.join(ZKPaths.PATH_SEPARATOR, "", "TEST", "WATCH");
		Publisher publisher = new Publisher(connectString);
		try {
			publisher.publish(ZKPaths.makePath(watchPath, "topic1"), "data1-0".getBytes());
			publisher.publish(ZKPaths.makePath(watchPath, "topic3"), "data3-0".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			fail("error!");
			return;
		}
		Subscriber.builder() //
				.connectString(connectString) //
				.watchPath(watchPath) //
				.onInitialized(Subscriber.LOGGING_ON_INITIALIZED) //
				.onAdded(Subscriber.LOGGING_ON_ADDED) //
				.onUpdated(Subscriber.LOGGING_ON_UPDATED) //
				.onRemoved(Subscriber.LOGGING_ON_REMOVED) //
				.build();
		try {
			publisher.publish(ZKPaths.makePath(watchPath, "topic1"), "data1-1".getBytes());
			publisher.publish(ZKPaths.makePath(watchPath, "topic2"), "data2-1".getBytes());
			publisher.publish(ZKPaths.makePath(watchPath, "topic3"), null);
		} catch (Exception e) {
			e.printStackTrace();
			fail("error!");
			return;
		}
		sleep(1000);
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}
