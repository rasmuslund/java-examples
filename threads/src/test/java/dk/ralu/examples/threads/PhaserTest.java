package dk.ralu.examples.threads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Phaser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class PhaserTest {

    private final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());

    private final Phaser phaser = new Phaser() {
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {

            String threadName = Thread.currentThread().getName();
            boolean terminate = false;
            System.out.println("[" + threadName + "] Before advancing (will terminate? " + terminate + "): "
                                       + PhaserTest.toString(this));

            // return super.onAdvance(phase, registeredParties);
            return terminate; // return true if this phaser should terminate
        }
    };

    @Test
    // Fails if it runs more than 12 seconds - nice when testing async stuff
    @Timeout(12)
    void doSomeStuffWithPhaser() throws InterruptedException {

        sleepAndLogPhaserStateAndThreadStates();

        phaser.bulkRegister(3);
        // wait: no
        // registered: +3
        // arrived: -

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: no
            // registered: -1
            // arrived: -
            // returns: the current phase
            logThreadMessage("phaser.arriveAndDeregister();");
            int result = phaser.arriveAndDeregister();
            logThreadMessage("phaser.arriveAndDeregister(); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: no (phase sent in as argument != current phase). We can see from the returned value that we waited for the wrong phase.
            // registered: -
            // arrived: -
            // returns: the current phase (if it HAD waited we would have waited until NEXT phase had started and would have been returned)
            logThreadMessage("phaser.awaitAdvance(1);");
            int result = phaser.awaitAdvance(1);
            logThreadMessage("phaser.awaitAdvance(1); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: yes (phase sent in as argument == current phase)
            // registered: -
            // arrived: -
            // returns: the value of the NEXT phase (which will be the current when the wait is over)
            logThreadMessage("phaser.awaitAdvance(0);");
            int result = phaser.awaitAdvance(0);
            logThreadMessage("phaser.awaitAdvance(0); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: yes
            // registered: -
            // arrived: +1
            // returns: the value of the NEXT phase (which will be the current when the wait is over)
            logThreadMessage("phaser.arriveAndAwaitAdvance();");
            int result = phaser.arriveAndAwaitAdvance();
            logThreadMessage("phaser.arriveAndAwaitAdvance(); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: no
            // registered: +1
            // arrived: -
            // returns: the current phase
            logThreadMessage("phaser.register();");
            int result = phaser.register();
            logThreadMessage("phaser.register(); --> " + result);

            logThreadMessage("phaser.arriveAndAwaitAdvance();");
            result = phaser.arriveAndAwaitAdvance();
            logThreadMessage("phaser.arriveAndAwaitAdvance(); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: yes
            // registered: -
            // arrived: +1
            logThreadMessage("phaser.arriveAndAwaitAdvance();");
            int result = phaser.arriveAndAwaitAdvance();
            logThreadMessage("phaser.arriveAndAwaitAdvance(); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: no
            // registered: -
            // arrived: +1
            // returns: the current phase
            logThreadMessage("phaser.arrive();");
            int result = phaser.arrive();
            logThreadMessage("phaser.arrive(); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: yes
            // registered: -
            // arrived: +1
            logThreadMessage("phaser.arriveAndAwaitAdvance();");
            int result = phaser.arriveAndAwaitAdvance();
            logThreadMessage("phaser.arriveAndAwaitAdvance(); --> " + result);
        });

        sleepAndLogPhaserStateAndThreadStates();

        createAndStartThread(() -> {
            // wait: no
            // registered: -
            // arrived: +1
            // returns: void
            logThreadMessage("phaser.forceTermination();");
            phaser.forceTermination();
            logThreadMessage("phaser.forceTermination(); --> void");
        });

        sleepAndLogPhaserStateAndThreadStates();
    }

    void createAndStartThread(Runnable runnable) {
        Thread thread = new Thread(() -> {
            logThreadMessage("started");
            runnable.run();
            logThreadMessage("stopping");
        });
        threads.add(thread);
        thread.setDaemon(true);
        thread.start();
    }

    private void logThreadMessage(String message) {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + threadName + "] " + message);
    }

    private void sleepAndLogPhaserStateAndThreadStates() throws InterruptedException {
        Thread.sleep(100);
        String msg = toString(phaser);
        synchronized (threads) {
            msg += "\n" + toString(threads);
        }
        System.out.println("\n" + msg + "\n");
    }

    private static String toString(List<Thread> threads) {
        if (threads.isEmpty()) {
            return "Threads [ ]";
        }
        StringBuilder msg = new StringBuilder("Threads [");
        for (Thread thread : threads) {
            msg.append("\n\t- ").append(thread.getName()).append(" [").append(thread.getState().toString().toLowerCase()).append("]");
        }
        return msg + "\n]";
    }

    private static String toString(Phaser phaser) {
        return "Phaser ["
                + "phase:" + phaser.getPhase()
                + ", registered:" + phaser.getRegisteredParties()
                + ", arrived:" + phaser.getArrivedParties()
                + ", isTerminated:" + phaser.isTerminated()
                + "]";
    }
}
