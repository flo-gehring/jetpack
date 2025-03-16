package de.flogehring.jetpack.construction;

import java.util.List;
import java.util.Optional;

public class ConstructionTest_VehicleGrammar {

    private static final String grammar = """
            Vehicle  <- Car / Train
            Car <- "Car" Engine Seats TÜV Extras?
            Engine <- (("Electric" Num "kWh/100km" )/ ("Gas" Num "l/100km" Num "nox" ) / ("Diesel" Num "l/100km")) Num "HP"
            Seats <- "Seats" Num
            TÜV <- ("Next HU" Num "/" Num) / ("n/a")
            Extras <- ("AWD" / "4WD")? "AC"? "CarPlay"?
            Train <- "Train" Waggons
            Waggons <- WaggonSpec+
            WaggonSpec <- PassengerWaggon / PowerWaggon
            PassengerWaggon <- ("Passenger" Num "Seats" Num "Standing" (Num "Bikes")?)
            PowerWaggon <- Engine
            Num <- "[0-9]+"
            """;





    sealed interface Vehicle {
        record Car(
                Engine engine,
                int numSeats,
                Optional<String> tüv,
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
