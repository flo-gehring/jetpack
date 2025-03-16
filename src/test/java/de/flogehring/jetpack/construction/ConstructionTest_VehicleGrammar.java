package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.parse.Grammar;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
        resolver.insert("Vehicle", Vehicle.class, vehicleFunction);
        return resolver;
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
        System.out.println(parsedCar);
    }

    @Test
    void train() {
        String car = "Train Electric 100 kWh100Km 1000 HP Passenger  50 Seats 100 Standing 5 Bikes Passenger 55 Seats 102 Standing";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsedCar = grammar.parse(car).getEither();
        System.out.println(parsedCar);
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
                Engine engine,
                List<Waggons> waggons
        ) implements Vehicle {
        }

    }

    sealed interface Waggons {
        record PowerCar(Engine engine) implements Waggons {

        }

        record PassengerCar(int seats, int standing, int bikes) {
        }
    }

    enum CarExtras {
        AWD,
        FOUR_WD,
        AC,
        CAR_PLAY
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
