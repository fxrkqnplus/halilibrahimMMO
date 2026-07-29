package com.halilibrahim.halilibrahimmmo.server;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

/** Owns {@code sunucu.yml} - the sleep and TAB-list behaviour switches. */
public interface ServerInfoService extends PluginService {

    ServerSettings settings();
}
