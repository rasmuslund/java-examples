package dk.ralu.examples.jool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jooq.lambda.function.Function0;
import org.jooq.lambda.function.Function1;
import org.jooq.lambda.function.Function2;
import org.jooq.lambda.function.Function3;
import org.junit.jupiter.api.Test;

class Functions {

    @Test
    void conversionBetweenJavaTypesAndJoolFunctions() {

        String string = "Rasmus";
        int stringLength = 6;

        Supplier<String> javaSupplier = () -> string;
        Function0<String> function0 = Function0.from(javaSupplier);
        Supplier<String> javaSupplierAgain = function0.toSupplier();

        assertThat(javaSupplier.get())
                .isEqualTo(string);

        assertThat(function0.apply()) // apply and get are synonyms on Function0
                .isEqualTo(string);

        assertThat(function0.get())
                .isEqualTo(string);

        assertThat(javaSupplierAgain.get())
                .isEqualTo(string);

        Function<String, Integer> javaFunction = String::length;
        Function1<String, Integer> function1 = Function1.from(javaFunction);
        Function<String, Integer> javaFunctionAgain = function1.toFunction();

        assertThat(javaFunction.apply(string))
                .isEqualTo(stringLength);

        assertThat(function1.apply(string))
                .isEqualTo(stringLength);

        assertThat(javaFunctionAgain.apply(string))
                .isEqualTo(stringLength);

        BiFunction<Integer, Integer, Integer> biFunction = Integer::sum;
        Function2<Integer, Integer, Integer> function2 = Function2.from(biFunction);
        BiFunction<Integer, Integer, Integer> biFunctionAgain = function2.toBiFunction();

        assertThat(biFunction.apply(2, 3))
                .isEqualTo(5);

        assertThat(function2.apply(2, 3))
                .isEqualTo(5);

        assertThat(biFunctionAgain.apply(2, 3))
                .isEqualTo(5);
    }

    @Test
    void functionsUtilClass() {

        Predicate<Integer> isEven = integer -> integer % 2 == 0;
        assertThat(isEven.test(4)).isTrue();
        assertThat(isEven.test(5)).isFalse();

        Predicate<Integer> notEven = org.jooq.lambda.function.Functions.not(isEven);
        assertThat(notEven.test(4)).isFalse();
        assertThat(notEven.test(5)).isTrue();

        Predicate<Integer> isNegative = integer -> integer < 0;
        Predicate<Integer> isEvenAndNegative = org.jooq.lambda.function.Functions.and(isEven, isNegative);
        assertThat(isEvenAndNegative.test(4)).isFalse();
        assertThat(isEvenAndNegative.test(-3)).isFalse();
        assertThat(isEvenAndNegative.test(-4)).isTrue();

        Predicate<Integer> isEvenOrNegative = org.jooq.lambda.function.Functions.or(isEven, isNegative);
        assertThat(isEvenOrNegative.test(3)).isFalse();
        assertThat(isEvenOrNegative.test(-3)).isTrue();
        assertThat(isEvenOrNegative.test(4)).isTrue();
    }

    @Test
    void applyPartial() {

        Function2<Integer, Integer, Integer> sum = Integer::sum;
        Function2<Integer, Integer, Integer> max = Integer::max;
        Function2<Integer, Integer, Integer> min = Integer::min;

        Function0<Integer> int1 = () -> 1;
        Function0<Integer> int2 = () -> 2;
        Function0<Integer> int3 = () -> 3;

        Function3<Function2<Integer, Integer, Integer>, Function0<Integer>, Function0<Integer>, Function0<Integer>> binaryExpression =
                (intBiFunction, intProvider1, intProvider2) ->
                        () -> intBiFunction.apply(intProvider1.get(), intProvider2.get());

        assertThat(binaryExpression.apply(max, int1, int2).apply()).isEqualTo(2);
        assertThat(binaryExpression.apply(min, int1, int2).apply()).isEqualTo(1);

        Function2<Function0<Integer>, Function0<Integer>, Function0<Integer>> sumExpression = binaryExpression
                .applyPartially(sum);

        assertThat(sumExpression.apply(int2, int3).apply()).isEqualTo(5);

        Function1<Function0<Integer>, Function0<Integer>> unaryIncrementExpression = sumExpression.applyPartially(int1);

        assertThat(unaryIncrementExpression.apply(int2).apply()).isEqualTo(3);

        assertThat(
                sumExpression.apply(
                        sumExpression.apply(int1, int2),
                        unaryIncrementExpression.apply(int3)
                ).apply()
        ).isEqualTo(7);
    }
}
