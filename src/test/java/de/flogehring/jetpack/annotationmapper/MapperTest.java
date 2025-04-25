package de.flogehring.jetpack.annotationmapper;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.parse.Grammar;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperTest {

    private static final String GRAMMAR_DEFINITION = """
            Vehicle  <- Car / Train
            Car <- "Car" Engine Seats TUEV Extras_List?
            Engine <- Electric /  Gas / Diesel
            Electric <- "Electric" Num "kWh100km" Num "HP"
            Gas <- "Gas" Num "l100km" Num "nox"  Num "HP"
            Diesel <- ("Diesel" Num "l100km") Num "HP"
            Seats <- "Seats" Num
            TUEV <- ("NextHU" Num "-" Num) / ("na")
            Extras_List <- Extras*
            Extras <- ("AWD" / "4WD") / "AC"  / "CAR_PLAY"
            Train <- "Train" Waggons
            Waggons <- WaggonSpec+
            WaggonSpec <- PassengerWaggon / PowerWaggon
            PassengerWaggon <- ("Passenger" Num "Seats" Num "Standing" (Num "Bikes")?)
            PowerWaggon <- Engine
            Num <- "[0-9]+"
            """;


    @FromRule("Vehicle")
    @OnRule(rule = "Car", clazz = Car.class)
    @OnRule(rule = "Train", clazz = Train.class)
    public interface Vehicle {

    }

    @FromRule("Car")
    @EqualsAndHashCode
    @ToString
    public static class Car implements Vehicle {

        public Car() {
        }

        @FromChild(index = 1)
        public Engine engine;
        @FromChild(index = 2)
        public Seats seats;
        @FromChild(index = 4)
        public List<Extra> extras;
    }


    @Test
    void test() throws Exception {

        String car = "Car Electric 10 kWh100Km 200 HP Seats 5 NextHu 10 - 25 AC CAR_PLAY";
        Grammar grammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> parsed = grammar.parse(car).getEither();
        Car expected = new Car();
        Electric electric = new Electric();
        electric.numHp = 200;
        expected.engine = electric;
        Seats seats = new Seats();
        seats.seats = 5;
        expected.seats = seats;
        expected.extras = List.of(Extra.AC, Extra.CAR_PLAY);
        assertEquals(expected, Mapper.map(parsed, Vehicle.class));
    }

    public enum Extra {
        AC,
        AWD,
        CAR_PLAY
    }


    @FromRule("Train")
    @EqualsAndHashCode
    @ToString
    public static class Train implements Vehicle {

    }

    @OnRule(rule = "Gas", clazz = Gas.class)
    @OnRule(rule = "Electric", clazz = Electric.class)
    public interface Engine {

    }

    @EqualsAndHashCode
    @ToString
    @FromRule("Electric")
    public static class Electric implements Engine {
        @FromChild(index = 3)
        public int numHp;
    }

    @EqualsAndHashCode
    @ToString
    @FromRule("Gas")
    public static class Gas implements Engine {
        public int numHp;
    }

    @FromRule("Seats")
    @ToString
    @EqualsAndHashCode
    public static class Seats {
        @FromChild(index = 1)
        public int seats;
    }
}