package dk.ralu.examples.jool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.junit.jupiter.api.Test;

class Sequences {

    @Test
    void convertBetweenSeqAndJavaTypes() {

        // Seq is a subtype of Java 8's Stream interface with many more useful methods

        List<String> words = Arrays.asList("here", "are", "some", "words");

        Seq<String> wordSeq1 = Seq.seq(words);
        Seq<String> wordSeq2 = Seq.seq(words.stream());
        Seq<String> wordSeq3 = Seq.seq(words.iterator());
        // And many more...

        Seq<String> wordSeq5 = Seq.of("even", "more", "words");

        Seq<String> wordSeq6 = wordSeq5.append("and", "more");
        Seq<String> wordSeq7 = wordSeq6.append(Arrays.asList("and", "on", "and", "on"));

        Set<String> backToJavaSetUsingCollector = wordSeq1.collect(Collectors.toSet());
        Set<String> backToJavaSet = wordSeq2.toUnmodifiableSet();
        List<String> backToJavaList = wordSeq3.toList();
        // And many more...

        assertThat(backToJavaList).isEqualTo(words);
    }

    @Test
    void groupBy() {

        // Location is: id, parent_id, name
        List<Tuple3<Integer, Integer, String>> locations = Arrays.asList(
                Tuple.tuple(1, null, "Europe"),
                Tuple.tuple(2, 1, "Denmark"),
                Tuple.tuple(3, 1, "Germany"),
                Tuple.tuple(4, 2, "Copenhagen"),
                Tuple.tuple(4, 2, "Kolding"),
                Tuple.tuple(5, 3, "Berlin")
        );

        Map<Integer, List<Tuple3<Integer, Integer, String>>> groups = Seq.seq(locations)
                .filter(location -> location.v2() != null)
                .groupBy(Tuple3::v2);

        List<String> namesOfLocationsInDenmark = groups.get(2).stream()
                .map(Tuple3::v3)
                .collect(Collectors.toList());

        assertThat(namesOfLocationsInDenmark)
                .containsExactlyInAnyOrder(
                        "Copenhagen",
                        "Kolding"
                );
    }

    @Test
    void selfJoin() {

        // Location is: id, parent_id, name
        List<Tuple3<Integer, Integer, String>> locations = Arrays.asList(
                Tuple.tuple(1, null, "Europe"),
                Tuple.tuple(2, 1, "Denmark"),
                Tuple.tuple(3, 1, "Germany"),
                Tuple.tuple(4, 2, "Copenhagen"),
                Tuple.tuple(4, 2, "Kolding"),
                Tuple.tuple(5, 3, "Berlin")
        );

        String result = Seq.seq(locations)
                .innerSelfJoin((self, parent) -> Objects.equals(self.v2(), parent.v1()))
                .map(resultTuple -> resultTuple.v1().v3() + " is located in " + resultTuple.v2().v3())
                .collect(Collectors.joining("\n"));

        assertThat(result)
                .isEqualTo(""
                                   + "Denmark is located in Europe\n"
                                   + "Germany is located in Europe\n"
                                   + "Copenhagen is located in Denmark\n"
                                   + "Kolding is located in Denmark\n"
                                   + "Berlin is located in Germany");
    }

    @Test
    void joins() {

        // Fields are: id, name
        List<Tuple2<Integer, String>> orders = Arrays.asList(
                Tuple.tuple(1, "First order"),
                Tuple.tuple(2, "Second order"),
                Tuple.tuple(3, "Third order") // order without any order lines
        );
        // Fields are: order_id, name, amount
        List<Tuple3<Integer, String, Integer>> orderLines = Arrays.asList(
                Tuple.tuple(1, "Pencil", 2),
                Tuple.tuple(1, "Cup", 1),
                Tuple.tuple(1, "Phone", 2),
                Tuple.tuple(2, "Pencil", 5),
                Tuple.tuple(2, "Paper", 100),
                Tuple.tuple(4, "Brush", 1) // order line without an order
        );

        BiPredicate<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>> joinPredicate =
                (order, orderLine) -> Objects.equals(order.v1(), orderLine.v1());

        Seq<Tuple2<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>>> join1 =
                Seq.seq(orders).innerJoin(Seq.seq(orderLines), joinPredicate);

        assertThat(toString(join1))
                .isEqualTo(""
                                   + "(1, First order, Pencil, 2)\n"
                                   + "(1, First order, Cup, 1)\n"
                                   + "(1, First order, Phone, 2)\n"
                                   + "(2, Second order, Pencil, 5)\n"
                                   + "(2, Second order, Paper, 100)\n"
                );

        Seq<Tuple2<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>>> join2 =
                Seq.seq(orders).leftOuterJoin(Seq.seq(orderLines), joinPredicate);

        assertThat(toString(join2))
                .isEqualTo(""
                                   + "(1, First order, Pencil, 2)\n"
                                   + "(1, First order, Cup, 1)\n"
                                   + "(1, First order, Phone, 2)\n"
                                   + "(2, Second order, Pencil, 5)\n"
                                   + "(2, Second order, Paper, 100)\n"
                                   + "(3, Third order, null, null)\n" // <-- note the nulls
                );

        Seq<Tuple2<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>>> join3 =
                Seq.seq(orders).rightOuterJoin(Seq.seq(orderLines), joinPredicate);

        assertThat(toString(join3))
                .isEqualTo(""
                                   + "(1, First order, Pencil, 2)\n"
                                   + "(1, First order, Cup, 1)\n"
                                   + "(1, First order, Phone, 2)\n"
                                   + "(2, Second order, Pencil, 5)\n"
                                   + "(2, Second order, Paper, 100)\n"
                                   + "(null, null, Brush, 1)\n" // <-- note the nulls
                );
    }

    private String toString(Seq<Tuple2<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>>> result) {
        Seq<Tuple4<Integer, String, String, Integer>> projectedResult = result.map(joinedTuple -> {
            Tuple2<Integer, String> order = joinedTuple.v1();
            Tuple3<Integer, String, Integer> orderLine = joinedTuple.v2();
            return Tuple.tuple(
                    order == null ? null : order.v1(),
                    order == null ? null : order.v2(),
                    orderLine == null ? null : orderLine.v2(),
                    orderLine == null ? null : orderLine.v3()
            );
        });
        StringBuilder sb = new StringBuilder();
        for (Tuple4<Integer, String, String, Integer> row : projectedResult) {
            sb.append(row.toString()).append('\n');
        }
        return sb.toString();
    }
}
