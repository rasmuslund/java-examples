package dk.ralu.examples.jool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.UncheckedException;
import org.jooq.lambda.fi.lang.CheckedRunnable;
import org.junit.jupiter.api.Test;

/**
 * Also see <a href="https://github.com/jOOQ/jOOL#orgjooqlambdaunchecked">https://github.com/jOOQ/jOOL</a>.
 */
class Exceptions {

    @Test
    void allowRunnableToThrowCheckedExceptions() {

        // Won't compile as Runnable.run() does not declare to throw the checked exceptions
        // Runnable runnable = () -> somethingThatMayThrowCheckedException();

        // Sneaky manages to trick the compiler into letting us do something, that may throw a checked exception... crazy stuff!
        assertThatThrownBy(() -> {
            Runnable runnable = CheckedRunnable.sneaky(() -> {
                throw new IOException("I'm a checked exception");
            });
            runnable.run();
        }).isExactlyInstanceOf(IOException.class);

        // A solution more in the spirit of Java, is to wrap the checked into an unchecked
        assertThatThrownBy(() -> {
            Runnable runnable = CheckedRunnable.unchecked(() -> {
                throw new CloneNotSupportedException("I'm a checked exception");
            });
            runnable.run();
        }).isExactlyInstanceOf(UncheckedException.class).hasCauseExactlyInstanceOf(CloneNotSupportedException.class);

        // The special variant of unchecked is used for IO exceptions
        assertThatThrownBy(() -> {
            Runnable runnable = CheckedRunnable.unchecked(() -> {
                throw new IOException("I'm a checked IO exception");
            });
            runnable.run();
        }).isExactlyInstanceOf(UncheckedIOException.class).hasCauseExactlyInstanceOf(IOException.class);

        // We can use a custom exception handler if we want to decide what type of exception to throw
        assertThatThrownBy(() -> {
            Runnable runnable = CheckedRunnable.unchecked(
                    () -> {
                        throw new IOException("I'm a checked IO exception");
                    },
                    throwable -> {
                        throw new RuntimeException("I decide the type of runtime ex to throw");
                    });
            runnable.run();
        }).isExactlyInstanceOf(RuntimeException.class);

        // Classes similar to CheckedRunnable exist for common Java functional interfaces, e.g.:'
        // - CheckedCallable
        // - CheckedComparator
        // - CheckedConsumer
        // - CheckedFunction
        //   etc.
    }

    @Test
    void theUncheckedUtilClass() {

        // The class Unchecked contains static helper methods for many of Java's functional interfaces

        // In the previous test we used: CheckedRunnable.unchecked(..)
        // Here we "flip" class and method: Unchecked.runnable(..)
        assertThatThrownBy(() -> {
            Runnable runnable = Unchecked.runnable(() -> {
                throw new CloneNotSupportedException("I'm a checked exception");
            });
            runnable.run();
        }).isExactlyInstanceOf(UncheckedException.class).hasCauseExactlyInstanceOf(CloneNotSupportedException.class);

        // For Callable
        assertThatThrownBy(() -> {
            Callable<Integer> callable = Unchecked.callable(() -> {
                throw new CloneNotSupportedException("I'm a checked exception");
            });
            Integer i = callable.call();
        }).isExactlyInstanceOf(UncheckedException.class).hasCauseExactlyInstanceOf(CloneNotSupportedException.class);

        // For Function
        assertThatThrownBy(() -> {
            Function<String, Integer> function = Unchecked.function((string) -> {
                throw new CloneNotSupportedException("I'm a checked exception");
            });
            Integer i = function.apply("hello");
        }).isExactlyInstanceOf(UncheckedException.class).hasCauseExactlyInstanceOf(CloneNotSupportedException.class);

        // Etc. for may of Java 8's built ind functional interface types
    }
}
