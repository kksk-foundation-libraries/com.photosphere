package com.photosphere.dynamic.generator;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkDynamicGenerator implements AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(ZkDynamicGenerator.class);

	private final ConcurrentMap<String, String> loadedAliases = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> loadedFqcns = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BiConsumer<String, String>> aliasListeners = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BiConsumer<String, String>> fqcnListeners = new ConcurrentHashMap<>();

	private final String connString;
	private final String watchPath;
	private final String dataPath;
	private final int depth;
	private Optional<CuratorFramework> curatorRef = Optional.empty();
	private final DynamicGenerator generator = DynamicGenerator.instance;
	private PathChildrenCache cache;

	public ZkDynamicGenerator(String connString, String watchPath, String dataPath) {
		this.connString = connString;
		this.watchPath = watchPath;
		this.dataPath = dataPath;
		this.depth = watchPath.split(ZKPaths.PATH_SEPARATOR).length + 1;
	}

	public synchronized void start() {
		if (curatorRef.isPresent())
			return;
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(100, 3);

		CuratorFramework client = CuratorFrameworkFactory.newClient(connString, retryPolicy);
		client.start();
		TreeCache cache = TreeCache //
				.newBuilder(client, watchPath) //
				.setMaxDepth(depth) //
				.setCacheData(false) //
				.disableZkWatches(false) //
				.build() //
		;
		cache.getListenable().addListener((c, event) -> {
			watch(event);
		});
		try {
			cache.start();
		} catch (Exception e) {
			LOG.error("unhandled error.", e);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("watching...:{}", watchPath);
		}
		try {
			client.getChildren().forPath(watchPath).forEach(t -> {
				try {
					load(t);
				} catch (Exception e) {
					LOG.error("load error", e);
				}
			});
		} catch (Exception e) {
			LOG.error("scan error", e);
		}
		curatorRef = Optional.of(client);
	}

	private void watch(TreeCacheEvent event) {
		try {
			TreeCacheEvent.Type type = event.getType();
			if (event.getData() == null)
				return;
			String path = event.getData().getPath();
			if (type == TreeCacheEvent.Type.NODE_ADDED && !watchPath.equals(path)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("event:{}", event);
				}
				String alias = ZKPaths.getNodeFromPath(path);
				LOG.debug("loading... alias:{}", alias);
				load(alias);
			}
		} catch (Exception e) {
			LOG.error("error on getting watched data", e);
		}
	}

	private void load(String alias) throws Exception {
		String fqcn = null;
		String source = null;
		byte[] buf;
		CuratorFramework client = curatorRef.get();
		buf = client.getData().forPath(ZKPaths.makePath(dataPath, alias, "fqcn"));
		if (buf == null)
			return;
		fqcn = new String(buf);
		buf = client.getData().forPath(ZKPaths.makePath(dataPath, alias, "source"));
		if (buf == null)
			return;
		source = new String(buf);
		try {
			Class.forName(fqcn);
			LOG.info("class [{}] has already loaded.", fqcn);
			return;
		} catch (ClassNotFoundException ignore) {
			// ignore
		}
		if (LOG.isInfoEnabled()) {
			LOG.info("loading. alias:{}, fqcn:{}", alias, fqcn);
		}
		generator.createInstance(source, fqcn, alias);
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

	@Override
	public void close() throws Exception {
		if (cache != null)
			cache.close();
		curatorRef.get().close();
	}
}
