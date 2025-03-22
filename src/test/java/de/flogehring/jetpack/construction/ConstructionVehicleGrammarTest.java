package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.parse.Grammar;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static de.flogehring.jetpack.grammar.Symbol.nonTerminal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstructionVehicleGrammarTest {

    private static final String GRAMMAR_DEFINITION = """
            Vehicle  <- Car / Train
            Car <- "Car" Engine Seats TUEV Extras*
            Engine <- (("Electric" Num "kWh100km") / ("Gas" Num "l100km" Num "nox" ) / ("Diesel" Num "l100km")) Num "HP"
            Seats <- "Seats" Num
            TUEV <- ("NextHU" Num "-" Num) / ("na")
            Extras <- ("AWD" / "4WD") / "AC"  / "CarPlay"
            Train <- "Train" Waggons
            Waggons <- WaggonSpec+
            WaggonSpec <- PassengerWaggon / PowerWaggon
            PassengerWaggon <- ("Passenger" Num "Seats" Num "Standing" (Num "Bikes")?)
            PowerWaggon <- Engine
            Num <- "[0-9]+"
            """;

    private RuleResolver getResolver() {
        RuleResolver resolver = RuleResolver.init();
        Function<Node<Symbol>, Vehicle> vehicleFunction = ResolverFunctionBuilder.init(Vehicle.class, resolver)
                .expectSingleNonTerminal()
                .delegateToResolver();
        resolver.insert(nonTerminal("Seats"), ResolverFunctionBuilder.init(Integer.class, resolver)
                .expectSingleNonTerminal()
                .delegateToResolver());
        Function<Node<Symbol>, Vehicle.Car> carRule = ResolverFunctionBuilder.init(Vehicle.Car.class, resolver)
                .composed()
                .from(ConstructionVehicleGrammarTest::getCar)
                .build();
        resolver.insert(nonTerminal("Num"), SelectorFunctions.getTerminalValue(0).andThen(Integer::valueOf));
        resolver.insert(nonTerminal("Car"), carRule);
        resolver.insert(nonTerminal("Vehicle"), vehicleFunction);
        resolver.insert(nonTerminal("Engine"), getEngineRule(resolver));
        resolver.insert(nonTerminal("TUEV"), ResolverFunctionBuilder.init(String.class, resolver)
                .ifThenElse()
                .ifThen(getWhen("na"), _ -> "n/a")
                .elseCase(ResolverFunctionBuilder.init(String.class, resolver).composed().from(
                        (r, s) ->
                                r.findChildAndApply(
                                                nonTerminal("Num"), 0, Integer.class)
                                        .apply(s)
                                        .toString() + "-" + r.findChildAndApply(
                                                nonTerminal("Num"), 1, Integer.class)
                                        .apply(s)
                                        .toString()
                ).build()));
        resolver.insert(nonTerminal("Extras"), SelectorFunctions.getTerminalValue(0).andThen(CarExtras::fromPrintable));

        resolver.insert(nonTerminal("Train"), getWaggons(resolver));
        resolver.insert(nonTerminal("Waggons"), ResolverFunctionBuilder.init(List.class, resolver)
                .composed()
                .from((r, n)
                        -> r.findListAndApply(new Symbol.NonTerminal("WaggonSpec"), Waggons.class)
                        .apply(n)
                ).build());
        resolver.insert(nonTerminal("WaggonSpec"),
                ResolverFunctionBuilder.init(Waggons.class, resolver).expectSingleNonTerminal()
                        .delegateToResolver()
        );
        resolver.insert(nonTerminal("PassengerWaggon"), ResolverFunctionBuilder.init(Waggons.PassengerCar.class, resolver)
                .composed()
                .from((r, node) ->
                        new Waggons.PassengerCar(
                                r.findChildAndApply(nonTerminal("Num"), 0, Integer.class)
                                        .apply(node),
                                r.findChildAndApply(nonTerminal("Num"), 1, Integer.class)
                                        .apply(node),
                                r.findOptionalAndApply(nonTerminal("Num"), 2, Integer.class)
                                        .apply(node)
                                        .orElse(0)

                        ))
                .build());
        resolver.insert(nonTerminal("PowerWaggon"), ResolverFunctionBuilder.init(Waggons.PowerCar.class, resolver)
                .composed()
                .from((r, node) -> new Waggons.PowerCar(
                        r.findChildAndApply(nonTerminal("Engine"), Engine.class)
                                .apply(node)
                )).build());
        return resolver;
    }

    @SuppressWarnings("unchecked")
    private static Function<Node<Symbol>, Vehicle.Train> getWaggons(RuleResolver resolver) {
        return ResolverFunctionBuilder.init(Vehicle.Train.class, resolver)
                .composed()
                .from((r, node) -> new Vehicle.Train(
                        (List<Waggons>) r.findChildAndApply(
                                new Symbol.NonTerminal("Waggons"), List.class
                        ).apply(node)
                ))
                .build();
    }

    private static Function<Node<Symbol>, Engine> getEngineRule(RuleResolver resolver) {
        return ResolverFunctionBuilder.init(Engine.class, resolver)
                .ifThenElse()
                .ifThen(getWhen("Electric"),
                        ResolverFunctionBuilder.init(Engine.class, resolver).composed()
                                .from((r, node) ->
                                        new Engine.Electric(
                                                r.findChildAndApply(new Symbol.NonTerminal("Num"), 1, Integer.class)
                                                        .apply(node)
                                        )).build())
                .ifThen(getWhen("Diesel"),
                        ResolverFunctionBuilder.init(Engine.class, resolver).composed()
                                .from((r, node) ->
                                        new Engine.Diesel(
                                                r.findChildAndApply(new Symbol.NonTerminal("Num"), 1, Integer.class)
                                                        .apply(node)
                                        )).build())
                .ifThen(getWhen("Gas"),
                        ResolverFunctionBuilder.init(Engine.class, resolver).composed()
                                .from((r, node) ->
                                        new Engine.Gas(
                                                r.findChildAndApply(new Symbol.NonTerminal("Num"), 2, Integer.class)
                                                        .apply(node),
                                                r.findChildAndApply(nonTerminal("Num"), 1, Integer.class)
                                                        .apply(node)
                                        )).build())
                .build();
    }

    private static Function<Node<Symbol>, Boolean> getWhen(String electric) {
        return SelectorFunctions.getTerminalValue(0).andThen(type -> type.equals(electric));
    }

    private static Vehicle.Car getCar(RuleResolver r, Node<Symbol> node) {
        return new Vehicle.Car(
                r.findChildAndApply(
                        nonTerminal("Engine"),
                        Engine.class
                ).apply(node),
                r.findChildAndApply(
                        nonTerminal("Seats"),
                        Integer.class
                ).apply(node),
                r.findOptionalAndApply(
                        nonTerminal("TUEV"),
                        String.class
                ).apply(node),
                r.findListAndApply(
                        nonTerminal("Extras"),
                        CarExtras.class
                ).apply(node));
    }

    @Test
    void carElectric() {
        String car = "Car Electric 10 kWh100Km 200 HP Seats 5 NextHu 10 - 25 AC CarPlay";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsedCar = grammar.parse(car).getEither();
        RuleResolver r = getResolver();
        Vehicle vehicle = r.get(nonTerminal("Vehicle"), Vehicle.class).apply(parsedCar);
        System.out.println(vehicle);
        assertEquals(new Vehicle.Car(new Engine.Electric(200), 5, Optional.of(
                "10-25"
        ), List.of(CarExtras.AC, CarExtras.CAR_PLAY)), vehicle);
    }

    @Test
    void carGas() {
        String car = "Car Gas 10 l100Km 5 nox 78 HP Seats 4 na";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsedCar = grammar.parse(car).getEither();
        RuleResolver r = getResolver();
        Vehicle vehicle = r.get(nonTerminal("Vehicle"), Vehicle.class).apply(parsedCar);
        assertEquals(new Vehicle.Car(new Engine.Gas(78, 5), 4, Optional.of("n/a"), List.of()), vehicle);
    }

    @Test
    void train() {
        String car = "Train Electric 100 kWh100Km 1000 HP Passenger  50 Seats 100 Standing 5 Bikes Passenger 55 Seats 102 Standing";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsedCar = grammar.parse(car).getEither();
        RuleResolver r = getResolver();
        Vehicle vehicle = r.get(nonTerminal("Vehicle"), Vehicle.class).apply(parsedCar);
        assertEquals(new Vehicle.Train(
                        List.of(
                                new Waggons.PowerCar(new Engine.Electric(1000)),
                                new Waggons.PassengerCar(50, 100, 5),
                                new Waggons.PassengerCar(55, 102, 0)
                        )
                ),
                vehicle);
    }

    sealed interface Vehicle {
        record Car(
                Engine engine,
                int numSeats,
                Optional<String> tuev,
                List<CarExtras> carExtras
        ) implements Vehicle {
        }

        record Train(
                List<Waggons> waggons
        ) implements Vehicle {
        }
    }

    sealed interface Waggons {
        record PowerCar(Engine engine) implements Waggons {

        }

        record PassengerCar(int seats, int standing, int bikes) implements Waggons {
        }
    }

    enum CarExtras {
        AWD("AWD"),
        FOUR_WD("4WD"),
        AC("AC"),
        CAR_PLAY("CarPlay");

        private final String name;

        CarExtras(String name) {
            this.name = name;
        }

        public static CarExtras fromPrintable(String name) {
            return Arrays.stream(CarExtras.values()).filter(
                            extra -> extra.name.equals(name)
                    ).findFirst()
                    .orElseThrow(() -> new RuntimeException("No car extra with name: " + name));
        }
    }

    sealed interface Engine {

        record Gas(int hp, int noxPpm) implements Engine {
        }

        record Electric(int hp) implements Engine {
        }

        record Diesel(int hp) implements Engine {
        }
    }
}