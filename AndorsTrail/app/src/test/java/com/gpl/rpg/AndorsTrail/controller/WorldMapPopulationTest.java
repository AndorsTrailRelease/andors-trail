package com.gpl.rpg.AndorsTrail.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.model.map.MapObject;
import com.gpl.rpg.AndorsTrail.model.map.MonsterSpawnArea;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class WorldMapPopulationTest {

	/** Queues the tasks and runs them one at a time when the test asks for it, like a serial executor. */
	private static final class QueueExecutor implements Executor {
		private final ArrayDeque<Runnable> queue = new ArrayDeque<Runnable>();

		@Override
		public void execute(Runnable task) {
			queue.add(task);
		}

		private void runNext() {
			queue.remove().run();
		}

		private void runAll() {
			while (!queue.isEmpty()) runNext();
		}
	}

	/** Generates the files of the maps in {@code missing}, and fails for the maps in {@code failing}. */
	private static final class FakeGenerator implements WorldMapController.WorldMapPopulation.MapFileGenerator {
		private final Set<String> missing = new HashSet<String>();
		private final Set<String> failing = new HashSet<String>();
		private final List<String> calls = new ArrayList<String>();
		private final List<String> generated = new ArrayList<String>();
		private Runnable onGenerate = null;

		@Override
		public boolean generateMissingFiles(PredefinedMap map) throws IOException {
			calls.add(map.name);
			if (failing.contains(map.name)) throw new IOException("Disk full");
			if (!missing.remove(map.name)) return false;
			generated.add(map.name);
			if (onGenerate != null) onGenerate.run();
			return true;
		}
	}

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	private final QueueExecutor executor = new QueueExecutor();
	private final FakeGenerator generator = new FakeGenerator();
	private final List<PredefinedMap> maps = Arrays.asList(createMap("a"), createMap("b"), createMap("c"));
	private WorldContext world;
	private File marker;

	@Before
	public void setUp() {
		world = new WorldContext();
		world.model = new ModelContainer(1, true);
		marker = new File(folder.getRoot(), "player");
	}

	@After
	public void tearDown() {
		WorldMapController.stopWorldMapPopulation();
	}

	private static PredefinedMap createMap(String name) {
		return new PredefinedMap(0, name, new Size(1, 1), new MapObject[0], new MonsterSpawnArea[0], Collections.<String>emptyList(), true, null);
	}

	private WorldMapController.WorldMapPopulation startPopulation() {
		WorldMapController.WorldMapPopulation population = new WorldMapController.WorldMapPopulation(executor, world, maps, generator, marker);
		WorldMapController.startPopulation(population);
		return population;
	}

	@Test
	public void markerIsWrittenOnlyAfterAllMapsWereProcessed() {
		generator.missing.addAll(Arrays.asList("a", "c"));
		startPopulation();

		executor.runNext();
		assertEquals(Arrays.asList("a"), generator.generated);
		assertFalse(marker.exists());

		executor.runNext();
		assertEquals(Arrays.asList("a", "c"), generator.generated);
		assertFalse(marker.exists());

		executor.runNext();
		assertTrue(marker.exists());
		assertTrue(executor.queue.isEmpty());
		assertEquals(Arrays.asList("a", "b", "c"), generator.calls);
	}

	@Test
	public void completeFilesAreMarkedInOneStep() {
		startPopulation();

		executor.runNext();

		assertTrue(marker.exists());
		assertTrue(executor.queue.isEmpty());
		assertTrue(generator.generated.isEmpty());
	}

	@Test
	public void tasksQueuedDuringAStepRunBeforeTheNextStep() {
		final List<String> order = new ArrayList<String>();
		generator.missing.addAll(Arrays.asList("a", "b"));
		generator.onGenerate = new Runnable() {
			@Override
			public void run() {
				order.add("generated " + generator.generated.get(generator.generated.size() - 1));
				if (generator.generated.size() == 1) {
					// For example a map transition, queued while the first map is rendered.
					executor.execute(new Runnable() {
						@Override
						public void run() {
							order.add("map transition");
						}
					});
				}
			}
		};
		startPopulation();

		executor.runAll();

		assertEquals(Arrays.asList("generated a", "map transition", "generated b"), order);
		assertTrue(marker.exists());
	}

	@Test
	public void failedMapIsNotMarkedAsCompleteAndIsRetriedByTheNextPopulation() {
		generator.missing.addAll(Arrays.asList("a", "b"));
		generator.failing.add("a");
		startPopulation();

		executor.runAll();

		assertEquals(Arrays.asList("b"), generator.generated);
		assertFalse(marker.exists());

		generator.failing.clear();
		startPopulation(); // At the next load of the savegame.
		executor.runAll();

		assertEquals(Arrays.asList("b", "a"), generator.generated);
		assertTrue(marker.exists());
	}

	@Test
	public void newPopulationStopsThePreviousOne() {
		generator.missing.addAll(Arrays.asList("a", "b", "c"));
		startPopulation();
		executor.runNext();
		assertEquals(Arrays.asList("a"), generator.calls);

		startPopulation();
		executor.runAll();

		// The previous population did not process any further map, so every map was processed once.
		assertEquals(Arrays.asList("a", "a", "b", "c"), generator.calls);
		assertEquals(Arrays.asList("a", "b", "c"), generator.generated);
		assertTrue(marker.exists());
	}

	@Test
	public void stoppedPopulationDoesNothing() {
		generator.missing.addAll(Arrays.asList("a", "b"));
		startPopulation();
		executor.runNext();

		WorldMapController.stopWorldMapPopulation(); // The world is reset, for example to load another savegame.
		executor.runAll();

		assertEquals(Arrays.asList("a"), generator.calls);
		assertFalse(marker.exists());
	}

	@Test
	public void populationStopsWhenAnotherWorldWasLoaded() {
		generator.missing.addAll(Arrays.asList("a", "b"));
		startPopulation();
		executor.runNext();

		world.model = new ModelContainer(1, true);
		executor.runAll();

		assertEquals(Arrays.asList("a"), generator.calls);
		assertFalse(marker.exists());
	}
}
