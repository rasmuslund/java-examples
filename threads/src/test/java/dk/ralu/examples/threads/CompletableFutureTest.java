package dk.ralu.examples.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompletableFutureTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletableFutureTest.class);

    private static final Random RANDOM = new Random();

    private static final String MY_THREAD_POOL_THREAD_NAME_PREFIX = "my-thread-pool-";
    private static final String FORK_JOIN_COMMON_THREAD_POOL_THREAD_NAME_PREFIX = "ForkJoinPool.commonPool-worker-";

    /**
     * Subtypes of Executor can be used with CompletableFuture, so let's create one backed by a thread pool with 5 threads in it.
     */
    private ExecutorService threadPool = Executors.newFixedThreadPool(5, new ThreadFactory() {

        private int nextThreadNumber = 0;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(MY_THREAD_POOL_THREAD_NAME_PREFIX + nextThreadNumber++);
            return thread;

        }
    });

    /**
     * A Runnable can be executed asynchronously using the built-in ForkJoinPool.commonPool().
     */
    @Test
    void asyncExecutionOfRunnableUsingCommonForkJoinPool() throws Exception {
        long startTime = System.currentTimeMillis();
        Runnable sleepThenPrintHelloWorldRunnable = () -> {
            assertCurrentThreadIsFromForkJoinCommonPool();
            sleep(100);
            LOGGER.info("Hello world");
        };
        CompletableFuture<Void> noResultFuture1 = CompletableFuture.runAsync(sleepThenPrintHelloWorldRunnable);
        CompletableFuture<Void> noResultFuture2 = CompletableFuture.runAsync(sleepThenPrintHelloWorldRunnable);
        noResultFuture1.get();
        noResultFuture2.get();
        long timeToRun = System.currentTimeMillis() - startTime;
        assertTrue(150 > timeToRun, "The 2 Runnable instances should complete faster than if they ran sequentially");
    }

    /**
     * Methods with "async" in their name, can be given an executor (aka thread pool) on which to run.
     */
    @Test
    void asyncExecutionOfRunnableUsingProvidedThreadPool() throws Exception {
        Runnable sleepThenPrintHelloWorldRunnable = () -> {
            assertCurrentThreadIsFromMyThreadPool();
            sleep(100);
            LOGGER.info("Hello world");
        };
        CompletableFuture<Void> noResultFuture1 = CompletableFuture.runAsync(sleepThenPrintHelloWorldRunnable, threadPool);
        CompletableFuture<Void> noResultFuture2 = CompletableFuture.runAsync(sleepThenPrintHelloWorldRunnable, threadPool);
        noResultFuture1.get();
        noResultFuture2.get();
    }

    /**
     * Static methods on CompletableFuture to create instances.
     */
    @Test
    void waysOfCreatingCompletableFutures() throws ExecutionException, InterruptedException {

        // Run an async computation that produces a result

        Supplier<String> greetingSupplier = () -> {
            sleep(100);
            return "Hello";
        };
        CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(greetingSupplier);

        // Run an async computation that does not produce a result

        Runnable runnable = () -> {
            sleep(100);
            System.out.println("Hello world");
        };

        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(runnable);

        // A future that has already completed

        CompletableFuture<String> alreadyCompletedCompletableFuture = CompletableFuture.completedFuture("Immediate result");

        // The first future that completes from amongst several futures

        CompletableFuture<Object> theFirstThatCompletesOfOtherFutures = CompletableFuture.anyOf(
                stringCompletableFuture,
                voidCompletableFuture,
                alreadyCompletedCompletableFuture);

        assertEquals("Immediate result", theFirstThatCompletesOfOtherFutures.get());

        // Wait for a group of futures to all complete

        CompletableFuture<Void> allFinishedVoidCompletableFuture = CompletableFuture.allOf(
                stringCompletableFuture,
                voidCompletableFuture,
                alreadyCompletedCompletableFuture);

        allFinishedVoidCompletableFuture.get(); // Nothing to get - but now we know that all the futures have completed

        assertTrue(stringCompletableFuture.isDone());
        assertTrue(voidCompletableFuture.isDone());
        assertTrue(alreadyCompletedCompletableFuture.isDone());
    }

    /**
     * Ways of specifying units of computation:
     * <ul>
     * <li>As a Supplier via the static "supplyAsync" method (output only)
     * <li>As a Runnable via "run" methods (neither input nor output) - e.g. thenRun(Runnable) or the static runAsync(Runnable)
     * <li>As a Function via "apply" methods (input & output) - e.g. thenApply(Function)
     * <li>As a Consumer via "accept" methods (input only) - e.g. thenAccept(Consumer)
     * </ul>
     */
    @Test
    void chainingCompletableFutures() throws ExecutionException, InterruptedException {

        // Accepts a Supplier
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            assertCurrentThreadIsFromForkJoinCommonPool();
            sleep(50);
            String initialString = "Took long to produce";
            LOGGER.info("Producing initial string: " + initialString);
            return initialString;
        });

        // Accepts a Function
        CompletableFuture<String> future2 = future1.thenApply(string -> { // non-async methods runs the step on same thread as previous step
            assertCurrentThreadIsFromForkJoinCommonPool();
            sleep(50);
            LOGGER.info("Converting to upper case");
            return string.toUpperCase();
        });

        // Accepts a Function
        CompletableFuture<String> future3 = future2.thenApplyAsync(string -> { // async methods runs the step on another thread
            assertCurrentThreadIsFromMyThreadPool();
            sleep(50);
            LOGGER.info("Converting to lower case");
            return string.toUpperCase();
        }, threadPool); // Note - we use our own thread pool here instead of the ForkJoinPool.commonPool()

        // Accepts a Consumer
        CompletableFuture<Void> future4 = future3.thenAcceptAsync(string -> { // async methods runs the step on another thread
            assertCurrentThreadIsFromForkJoinCommonPool();
            sleep(50);
            LOGGER.info("Final result is complete: " + string);
        }); // as we don't provide an executor as parameter, the job will be run on the ForkJoinPool.commonPool()

        future4.get(); // block until everything is complete
    }

    /**
     * Use "combine" methods to wait for 2 futures to complete and join them into one.
     */
    @Test
    void combiningFuturesIntoSingleFuture() throws Exception {

        CompletableFuture<Integer> birthYearFetcher = CompletableFuture.supplyAsync(() -> {
            sleep(50);
            return 1989;
        });

        CompletableFuture<String> nameFetcher = CompletableFuture.supplyAsync(() -> {
            sleep(25);
            return "Olaf";
        });

        // First parameter is the other CompletableFuture to combine with
        // Second parameter is a BiFunction, which takes the results of the 2 CompletableFutures and merge them into a single result
        CompletableFuture<String> fetchedResultsCombiner = birthYearFetcher.thenCombine(nameFetcher, (birthYear, name) -> {
            sleep(60);
            return name + " was born in " + birthYear;
        });

        String sentence = fetchedResultsCombiner.get();
        LOGGER.info(sentence);
        assertEquals("Olaf was born in 1989", sentence);
    }

    /**
     * Use "either" methods to wait for the first of 2 futures to complete and use its result in a new computation.
     * <p>
     * The results they produce must be of the same type.
     */
    @Test
    void usingQuickestFutureAsResult() throws Exception {

        CompletableFuture<Integer> firstFetcher = CompletableFuture.supplyAsync(() -> {
            sleepBetween(15, 45);
            return 20;
        });

        CompletableFuture<Integer> secondFetcher = CompletableFuture.supplyAsync(() -> {
            sleepBetween(10, 50);
            return 10;
        });

        // First parameter is the other CompletableFuture to combine with
        // Second parameter is a BiFunction, which takes the results of the 2 CompletableFutures and merge them into a single result
        CompletableFuture<String> fetchedResultsCombiner = firstFetcher.applyToEither(secondFetcher, firstAvailableResult -> {
            sleep(60);
            return "The first result available was: " + firstAvailableResult;
        });

        String result = fetchedResultsCombiner.get();
        LOGGER.info(result);
        assertTrue(result.equals("The first result available was: 10") || result.equals("The first result available was: 20"));
    }

    private void assertCurrentThreadIsFromMyThreadPool() {
        assertCurrentThreadNameStartsWith(MY_THREAD_POOL_THREAD_NAME_PREFIX);
    }

    private void assertCurrentThreadIsFromForkJoinCommonPool() {
        assertCurrentThreadNameStartsWith(FORK_JOIN_COMMON_THREAD_POOL_THREAD_NAME_PREFIX);
    }

    private void assertCurrentThreadNameStartsWith(String expectedThreadNamePrefix) {
        String actualThreadName = Thread.currentThread().getName();
        assertTrue(actualThreadName.startsWith(expectedThreadNamePrefix),
                   "Thread name should start with " + expectedThreadNamePrefix + " but was: " + actualThreadName);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }

    private void sleepBetween(int minMillis, int maxMillis) {
        try {
            Thread.sleep(minMillis + RANDOM.nextInt(maxMillis - minMillis));
        } catch (InterruptedException ignore) {
        }
    }
}