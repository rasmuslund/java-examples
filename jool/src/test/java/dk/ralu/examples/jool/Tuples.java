package dk.ralu.examples.jool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.lambda.function.Function1;
import org.jooq.lambda.function.Function2;
import org.jooq.lambda.function.Function3;
import org.jooq.lambda.tuple.Range;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple0;
import org.jooq.lambda.tuple.Tuple1;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple5;
import org.jooq.lambda.tuple.Tuple8;
import org.jooq.lambda.tuple.Tuple9;
import org.junit.jupiter.api.Test;

class Tuples {

    @Test
    void tuplesAsFunctionArguments() {

        Function3<Character, Integer, Integer, String> mathBiOperationToString = (operand, a, b) -> a + " " + operand + " " + b;
        assertThat(mathBiOperationToString.apply('+', 2, 3)).isEqualTo("2 + 3");

        Tuple3<Character, Integer, Integer> arguments = Tuple.tuple('/', 6, 2);
        assertThat(mathBiOperationToString.apply(arguments)).isEqualTo("6 / 2");

        Function2<Integer, Integer, String> minusToString = mathBiOperationToString.applyPartially('-');
        Tuple2<Integer, Integer> numberPair = Tuple.tuple(4, 2);
        assertThat(minusToString.apply(numberPair)).isEqualTo("4 - 2");

        Tuple2<Character, Integer> exp2 = Tuple.tuple('^', 2);
        Function1<Integer, String> exp2toString = mathBiOperationToString.applyPartially(exp2);
        assertThat(exp2toString.apply(5)).isEqualTo("2 ^ 5");
    }

    @Test
    void tupleBasics() {

        // Note that each position in a tuple has a type, and those type stay the same when joining tuples or splitting up tuples

        Tuple2<Integer, String> tupleA = new Tuple2<>(4, "four");
        assertThat(tupleA.v1()).isEqualTo(4);
        assertThat(tupleA.v2()).isEqualTo("four");

        Tuple2<Integer, String> tupleB = Tuple.tuple(2, "two");
        assertThat(tupleA.equals(tupleB)).isFalse();

        Tuple2<Integer, String> tupleC = Tuple.tuple(4, "four");
        assertThat(tupleA.equals(tupleC)).isTrue();

        assertThat(tupleA.compareTo(tupleB)).isPositive();
        assertThat(tupleA.compareTo(tupleC)).isEqualTo(0);
        assertThat(tupleB.compareTo(tupleA)).isNegative();

        Tuple5<Integer, Character, String, String, Long> bigTuple = Tuple.tuple(1, 'b', "B", "Hey", 4L);

        assertThat(bigTuple.degree()).isEqualTo(5);

        Tuple5<Integer, Character, String, String, Long> concat = Tuple.tuple(1, 'b', "B").concat(Tuple.tuple("Hey", 4L));
        assertThat(bigTuple.equals(concat)).isTrue();

        Tuple0 limit0 = bigTuple.limit0();
        assertThat(limit0.equals(Tuple.tuple())).isTrue();

        Tuple1<Integer> limit1 = bigTuple.limit1();
        assertThat(limit1.equals(Tuple.tuple(1))).isTrue();

        Tuple2<Integer, Character> limit2 = bigTuple.limit2();
        assertThat(limit2.equals(Tuple.tuple(1, 'b'))).isTrue();

        Tuple3<String, String, Long> skip2 = bigTuple.skip2();
        assertThat(skip2.equals(Tuple.tuple("B", "Hey", 4L))).isTrue();

        Tuple2<Tuple3<Integer, Character, String>, Tuple2<String, Long>> twoTuples = bigTuple.split3();
        Tuple3<Integer, Character, String> headPart = twoTuples.v1();
        Tuple2<String, Long> tailPart = twoTuples.v2();
        assertThat(headPart.concat(tailPart).equals(bigTuple)).isTrue();
    }

    @Test
    void mapValuesInTuple() {

        Tuple3<Integer, Boolean, String> tuple = Tuple.tuple(3, true, "Hello");

        // Map value 1 to another value of same type
        Tuple3<Integer, Boolean, String> tupleA = tuple.map1(integer -> integer * 2);
        assertThat(tupleA.v1()).isEqualTo(6);

        // Map value 1 to another value of another type
        Tuple3<Object, Boolean, String> tupleB = tuple.map1(integer -> integer >= 0);
        assertThat(tupleB.v1()).isEqualTo(true);

        // Map the 3rd value to another value of another type
        Tuple3<Integer, Boolean, Boolean> tupleC = tuple.map3(string -> string.length() < 3);
        assertThat(tupleC.v3()).isEqualTo(false);

        // Map all the values in a tuple to a single value using a Function3 (function takes same number of arguments as the tuple's size)
        Function3<Integer, Boolean, String, String> toStringFunction = (v1, v2, v3) -> v1 + "~" + v2 + "~" + v3;
        String result = tuple.map(toStringFunction);
        assertThat(result).isEqualTo("3~true~Hello");
    }

    @Test
    void tupleToUntypedJavaCollection() {

        // Note that mapping the items in a tuple to a Java collection type "looses" the position specific type.

        Tuple5<Integer, Character, String, String, Long> bigTuple = Tuple.tuple(1, 'b', "B", "Hey", 4L);

        Iterator<Object> iterator = bigTuple.iterator();
        assertThat(iterator).isInstanceOf(Iterator.class);

        Object[] array = bigTuple.toArray();
        assertThat(array.length).isEqualTo(5);

        List<?> list = bigTuple.toList();
        assertThat(list.get(2)).isEqualTo("B");

        // With keys v1, v2, v3, etc. (for value1, value2, etc.)
        Map<String, ?> map = bigTuple.toMap();
        assertThat(map.get("v2")).isEqualTo('b');
    }

    @Test
    void rangeTuple() {

        // Range is a special kind of tuple with 2 values of the same type, that must be comparable

        Range<String> stringRange = new Range<>("a", "d");
        assertThat(stringRange.v1()).isEqualTo("a");
        assertThat(stringRange.v2()).isEqualTo("d");

        // Supports extra methods compared to plain tuples

        Range<Integer> intRange1 = new Range<>(30, 42);
        Range<Integer> intRange2 = new Range<>(47, 56);
        Range<Integer> intRange3 = new Range<>(54, 58);
        Range<Integer> intRange4 = new Range<>(55, 57);

        assertThat(intRange1.intersect(intRange2)).isEqualTo(Optional.empty());

        assertThat(intRange2.intersect(intRange3)).isEqualTo(Optional.of(new Range<>(54, 56)));
        assertThat(intRange3.intersect(intRange4)).isEqualTo(Optional.of(new Range<>(55, 57)));

        assertThat(intRange1.overlaps(intRange2)).isFalse();
        assertThat(intRange2.overlaps(intRange3)).isTrue();
    }
}
