package com.godaddy.vps4.snapshot;

import com.godaddy.vps4.snapshot.jdbc.JdbcSnapshotActionService;
import com.godaddy.vps4.snapshot.jdbc.JdbcSnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class SnapshotModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(SnapshotService.class).to(JdbcSnapshotService.class);
		bind(ActionService.class).annotatedWith(Names.named("Snapshot_action")).to(JdbcSnapshotActionService.class);
	}
}
