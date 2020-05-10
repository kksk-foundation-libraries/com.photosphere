package com.photosphere.dynamic.generator;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.utils.ZKPaths;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkDynamicGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(ZkDynamicGenerator.class);

	private final ConcurrentMap<String, String> loadedAliases = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> loadedFqcns = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BiConsumer<String, String>> aliasListeners = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BiConsumer<String, String>> fqcnListeners = new ConcurrentHashMap<>();

	private final String connString;
	private final String watchPath;
	private Optional<CuratorFramework> curatorRef = Optional.empty();
	private final DynamicGenerator generator = new DynamicGenerator();

	public ZkDynamicGenerator(String connString, String watchPath) {
		this.connString = connString;
		this.watchPath = watchPath;
	}

	public synchronized void start() {
		if (curatorRef.isPresent())
			return;
		int sleepMsBetweenRetries = 100;
		int maxRetries = 3;
		RetryPolicy retryPolicy = new RetryNTimes(maxRetries, sleepMsBetweenRetries);

		CuratorFramework client = CuratorFrameworkFactory.newClient(connString, retryPolicy);
		client.start();
		curatorRef = Optional.of(client);
		AsyncCuratorFramework async = AsyncCuratorFramework.wrap(client);
		async //
				.watched() //
				.getData() //
				.forPath(watchPath) //
				.event() //
				.thenAccept(this::watch) //
		;
	}

	public boolean existsAlias(String alias) {
		return loadedAliases.containsKey(alias);
	}

	public boolean existsFqcn(String fqcn) {
		return loadedFqcns.containsKey(fqcn);
	}

	public void addListener(String alias, String fqcn, BiConsumer<String, String> listener) {
		if (alias != null) {
			aliasListeners.put(alias, listener);
		}
		if (fqcn != null) {
			fqcnListeners.put(fqcn, listener);
		}
	}

	public void removeListener(String alias, String fqcn) {
		if (alias != null) {
			aliasListeners.remove(alias);
		}
		if (fqcn != null) {
			fqcnListeners.remove(fqcn);
		}
	}

	private void watch(WatchedEvent event) {
		try {
			EventType type = event.getType();
			if (type == EventType.NodeCreated || type == EventType.NodeChildrenChanged) {
				int pos = event.getPath().lastIndexOf("/");
				String parent = event.getPath().substring(0, pos - 1);
				String name = event.getPath().substring(pos + 1);

				String alias = null;
				String fqcn = null;
				String source = null;
				byte[] buf;
				CuratorFramework client = curatorRef.get();
				if (!"alias".equals(name) && !"fqcn".equals(name) && !"source".equals(name)) {
					parent = event.getPath();
				}
				buf = client.getData().forPath(ZKPaths.makePath(parent, "alias"));
				if (buf == null)
					return;
				alias = new String(buf);
				buf = client.getData().forPath(ZKPaths.makePath(parent, "fqcn"));
				if (buf == null)
					return;
				fqcn = new String(buf);
				buf = client.getData().forPath(ZKPaths.makePath(parent, "source"));
				if (buf == null)
					return;
				source = new String(buf);
				try {
					Class.forName(fqcn);
					LOG.info("class [{}] has already loaded.");
					return;
				} catch (ClassNotFoundException ignore) {
					// ignore
				}
				generator.newInstance(source, fqcn);
				loadedAliases.put(alias, alias);
				loadedFqcns.put(fqcn, fqcn);
				BiConsumer<String, String> consumer1 = aliasListeners.get(alias);
				BiConsumer<String, String> consumer2 = fqcnListeners.get(fqcn);
				if (consumer1 != null) {
					consumer1.accept(alias, fqcn);
				}
				if (consumer2 != null && consumer1 != consumer2) {
					consumer2.accept(alias, fqcn);
				}
			}
		} catch (Exception e) {
			LOG.error("error on getting watched data", e);
		}
	}
}
