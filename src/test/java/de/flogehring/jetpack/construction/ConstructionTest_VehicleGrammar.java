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

public class ConstructionTest_VehicleGrammar {

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
                .onRule("Train").delegateToResolver()
                .onRule("Car").delegateToResolver()
                .build();
        resolver.insert("Vehicle", vehicleFunction);
        resolver.insert("Seats", ResolverFunctionBuilder.init(Integer.class, resolver)
                .expectSingleNonTerminal()
                .onRule("Num").delegateToResolver()
                .build());
        Function<Node<Symbol>, Vehicle.Car> carRule = ResolverFunctionBuilder.init(Vehicle.Car.class, resolver)
                .composed()
                .from(ConstructionTest_VehicleGrammar::getCar)
                .build();
        Function<Node<Symbol>, Engine> engineRule = ResolverFunctionBuilder.init(Engine.class, resolver)
                .cases()
                .ifThen(
                        SelectorFunctions.getTerminalValue(0).andThen(type -> type.equals("Electric")),
                        ResolverFunctionBuilder.init(Engine.class, resolver).composed()
                                .from((r, node) ->
                                        new Engine.Electric(
                                                r.findChildAndApply(new Symbol.NonTerminal("Num"), 1, "Num", Integer.class)
                                                        .apply(node)
                                        )).build())
                .ifThen(
                        SelectorFunctions.getTerminalValue(0).andThen(type -> type.equals("Diesel")),
                        ResolverFunctionBuilder.init(Engine.class, resolver).composed()
                                .from((r, node) ->
                                        new Engine.Diesel(
                                                r.findChildAndApply(new Symbol.NonTerminal("Num"), 1, "Num", Integer.class)
                                                        .apply(node)
                                        )).build())
                .ifThen(
                        SelectorFunctions.getTerminalValue(0).andThen(type -> type.equals("Gas")),
                        ResolverFunctionBuilder.init(Engine.class, resolver).composed()
                                .from((r, node) ->
                                        new Engine.Diesel(
                                                r.findChildAndApply(new Symbol.NonTerminal("Num"), 2, "Num", Integer.class)
                                                        .apply(node)
                                        )).build())
                .build();
        resolver.insert("Num", SelectorFunctions.getTerminalValue(0).andThen(Integer::valueOf));
        resolver.insert("Car", carRule);
        resolver.insert("Engine", engineRule);
        resolver.insert("TUEV", ResolverFunctionBuilder.init(String.class, resolver)
                .cases()
                .ifThen(SelectorFunctions.getTerminalValue(0).andThen(s -> s.equals("na")),
                        _ -> "n/a"
                )
                .elseCase(ResolverFunctionBuilder.init(String.class, resolver).composed().from(
                        (r, s) ->
                                r.findChildAndApply(
                                                nonTerminal("Num"), 0, "Num", Integer.class)
                                        .apply(s)
                                        .toString() + " - " + r.findChildAndApply(
                                                nonTerminal("Num"), 1, "Num", Integer.class)
                                        .apply(s)
                                        .toString()
                ).build()));
        resolver.insert("Extra", SelectorFunctions.getTerminalValue(0).andThen(CarExtras::fromPrintable));
        resolver.insert("Train", ResolverFunctionBuilder.init(Vehicle.Train.class, resolver).composed()
                .from((r, node) -> new Vehicle.Train(
                        (List<Waggons>) r.findChildAndApply(
                                new Symbol.NonTerminal("Waggons"), "Waggons", List.class
                        ).apply(node)
                ))
                .build());
        resolver.insert("Waggons", ResolverFunctionBuilder.init(List.class, resolver)
                .composed()
                .from((r, n)
                        -> r.findListAndApply(new Symbol.NonTerminal("WaggonSpec"), "WaggonSpec", Waggons.class)
                        .apply(n)
                ).build());
        resolver.insert("WaggonSpec",
                ResolverFunctionBuilder.init(Waggons.class, resolver).expectSingleNonTerminal()
                        .onRule("PassengerWaggon").delegateToResolver()
                        .onRule("PowerWaggon").delegateToResolver()
                        .build());
        resolver.insert("PassengerWaggon", _ -> new Waggons.PassengerCar(2, 1, 1));
        resolver.insert("PowerWaggon", _ -> new Waggons.PowerCar(new Engine.Electric(12)));
        return resolver;
    }

    private static Vehicle.Car getCar(RuleResolver r, Node<Symbol> node) {
        return new Vehicle.Car(
                r.findChildAndApply(
                        nonTerminal("Engine"),
                        "Engine",
                        Engine.class
                ).apply(node),
                r.findChildAndApply(
                        nonTerminal("Seats"),
                        "Seats",
                        Integer.class
                ).apply(node),
                r.findOptionalAndApply(
                        nonTerminal("TUEV"),
                        "TUEV",
                        String.class
                ).apply(node),
                r.findListAndApply(
                        nonTerminal("Extras"),
                        "Extra",
                        CarExtras.class
                ).apply(node));
    }

    @Test
    void carElectric() {
        String car = "Car Electric 10 kWh100Km 200 HP Seats 5 NextHu 10 - 25 AC CarPlay";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsedCar = grammar.parse(car).getEither();
        RuleResolver r = getResolver();
        Vehicle vehicle = r.get("Vehicle", Vehicle.class).apply(parsedCar);
        System.out.println(vehicle);
        System.out.println(parsedCar);
    }

    @Test
    void carGas() {
        String car = "Car Gas 10 l100Km 5 nox 78 HP Seats 4 NextHu 9 - 22";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsedCar = grammar.parse(car).getEither();
        RuleResolver r = getResolver();
        Vehicle vehicle = r.get("Vehicle", Vehicle.class).apply(parsedCar);
        System.out.println(vehicle);
    }

    @Test
    void train() {
        String car = "Train Electric 100 kWh100Km 1000 HP Passenger  50 Seats 100 Standing 5 Bikes Passenger 55 Seats 102 Standing";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsedCar = grammar.parse(car).getEither();
        RuleResolver r = getResolver();
        Vehicle vehicle = r.get("Vehicle", Vehicle.class).apply(parsedCar);
        System.out.println(vehicle);
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
        int hp();

        record Gas(int hp, int noxPpm) implements Engine {
        }

        record Electric(int hp) implements Engine {
        }

        record Diesel(int hp) implements Engine {
        }
    }
}
