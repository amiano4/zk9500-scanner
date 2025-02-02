package com.sparkcleancebu.zk9500;

import java.util.EventListener;

public interface ReadEventListener extends EventListener {
	void readEventOccured(ReadEvent event);
}

