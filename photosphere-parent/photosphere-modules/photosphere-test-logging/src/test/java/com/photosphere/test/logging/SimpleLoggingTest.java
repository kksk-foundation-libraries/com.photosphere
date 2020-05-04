package com.photosphere.test.logging;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLoggingTest {
	static {
		SimpleLogging.of() //
				.showDateTime() //
				.showShortLogName() //
				.showThreadName() //
				.dateTimeFormat("yyyy-MM-dd HH:mm:ss:SSS") //
//				.defalutLogLevel("DEBUG") //
//				.loggingGroup("HOGEHOGE") //
				.loggingGroupLogLevel("DEBUG") //
				.levelInBrackets() //
				.initialize() //
		;
	}
	private static final Logger LOG = LoggerFactory.getLogger(SimpleLoggingTest.class);

	@Test
	public void test() {
		LOG.debug("debug message");
		LOG.info("info message");
	}

}
