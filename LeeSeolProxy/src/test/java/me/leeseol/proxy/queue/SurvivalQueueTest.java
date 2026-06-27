package me.leeseol.proxy.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public final class SurvivalQueueTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    public void enqueuesPlayersInFifoOrder() {
        SurvivalQueue queue = new SurvivalQueue();

        assertEquals(new SurvivalQueue.Registration(true, 1), queue.enqueue(FIRST, "first"));
        assertEquals(new SurvivalQueue.Registration(true, 2), queue.enqueue(SECOND, "second"));

        assertEquals(Optional.of(new SurvivalQueue.Entry(FIRST, "first")), queue.poll());
        assertEquals(Optional.of(new SurvivalQueue.Entry(SECOND, "second")), queue.poll());
    }

    @Test
    public void duplicateRegistrationKeepsOriginalPosition() {
        SurvivalQueue queue = new SurvivalQueue();

        assertEquals(new SurvivalQueue.Registration(true, 1), queue.enqueue(FIRST, "first"));
        assertEquals(new SurvivalQueue.Registration(false, 1), queue.enqueue(FIRST, "renamed"));

        assertEquals(1, queue.size());
        assertEquals(Optional.of(new SurvivalQueue.Entry(FIRST, "first")), queue.peek());
    }

    @Test
    public void failedMoveCanBeRequeuedAtBack() {
        SurvivalQueue queue = new SurvivalQueue();
        queue.enqueue(FIRST, "first");
        queue.enqueue(SECOND, "second");

        SurvivalQueue.Entry failed = queue.poll().orElseThrow();
        queue.requeueAtBack(failed);

        assertEquals(Optional.of(new SurvivalQueue.Entry(SECOND, "second")), queue.poll());
        assertEquals(Optional.of(new SurvivalQueue.Entry(FIRST, "first")), queue.poll());
    }

    @Test
    public void removingPlayerShiftsRemainingPositions() {
        SurvivalQueue queue = new SurvivalQueue();
        queue.enqueue(FIRST, "first");
        queue.enqueue(SECOND, "second");

        assertTrue(queue.remove(FIRST));
        assertFalse(queue.remove(FIRST));

        assertEquals(1, queue.positionOf(SECOND));
        assertEquals(0, queue.positionOf(FIRST));
    }
}
